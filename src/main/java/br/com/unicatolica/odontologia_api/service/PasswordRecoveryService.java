package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.model.PasswordResetToken;
import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.PasswordResetTokenRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import br.com.unicatolica.odontologia_api.security.PasswordResetCodeSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordRecoveryService {

    public static final String MENSAGEM_NEUTRA = "Se os dados corresponderem a uma conta elegível, enviaremos instruções para o e-mail cadastrado.";
    private static final int MAX_TENTATIVAS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final PasswordResetCodeSender sender;
    private final Clock clock;

    @Autowired
    public PasswordRecoveryService(UsuarioRepository usuarios,
                                   PasswordResetTokenRepository tokens,
                                   PasswordEncoder encoder,
                                   PasswordResetCodeSender sender) {
        this(usuarios, tokens, encoder, sender, Clock.systemUTC());
    }

    PasswordRecoveryService(UsuarioRepository usuarios,
                           PasswordResetTokenRepository tokens,
                           PasswordEncoder encoder,
                           PasswordResetCodeSender sender,
                           Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.sender = sender;
        this.clock = clock;
    }

    @Transactional
    public void solicitar(String identificador) {
        Usuario usuario = resolverUsuario(identificador).orElse(null);
        if (!elegivel(usuario)) return;

        tokens.invalidarNaoUsados(usuario.getId());
        String codigo = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuarioId(usuario.getId());
        token.setCodigoHash(encoder.encode(codigo));
        token.setExpiraEm(OffsetDateTime.now(clock).plusMinutes(15));
        token.setUsado(false);
        token.setTentativasFalhas(0);
        tokens.saveAndFlush(token);

        try {
            sender.enviar(usuario.getId(), usuario.getEmail(), usuario.getNome(), codigo);
        } catch (RuntimeException ignored) {
            // Não revelar falhas de entrega nem a elegibilidade da conta na resposta pública.
        }
    }

    @Transactional
    public boolean redefinir(String identificador, String codigo, String novaSenha) {
        Usuario usuario = resolverUsuario(identificador).filter(this::elegivel).orElse(null);
        if (usuario == null) return false;

        PasswordResetToken token = tokens.buscarAbertoParaAtualizacao(usuario.getId()).orElse(null);
        if (token == null || Boolean.TRUE.equals(token.getUsado())) return false;

        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (token.getExpiraEm() == null || !token.getExpiraEm().isAfter(agora)) {
            token.setUsado(true);
            tokens.save(token);
            return false;
        }
        if (token.getTentativasFalhas() >= MAX_TENTATIVAS) {
            token.setUsado(true);
            tokens.save(token);
            return false;
        }
        if (codigo == null || !encoder.matches(codigo, token.getCodigoHash())) {
            int tentativas = token.getTentativasFalhas() + 1;
            token.setTentativasFalhas(tentativas);
            if (tentativas >= MAX_TENTATIVAS) token.setUsado(true);
            tokens.save(token);
            return false;
        }
        if (novaSenha == null || novaSenha.length() < 12 || novaSenha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return false;
        }

        Usuario usuarioAtual = usuarios.buscarPorIdParaAtualizacao(usuario.getId()).orElse(null);
        if (!elegivel(usuarioAtual)) return false;
        usuarioAtual.setSenhaHash(encoder.encode(novaSenha));
        usuarios.save(usuarioAtual);
        token.setUsado(true);
        tokens.save(token);
        return true;
    }

    private Optional<Usuario> resolverUsuario(String informado) {
        if (informado == null || informado.isBlank()) return Optional.empty();
        String identificador = informado.trim();
        Optional<Usuario> porEmail = usuarios.findByEmailIgnoreCase(identificador.toLowerCase(Locale.ROOT));
        Optional<Usuario> porMatricula = usuarios.findByMatriculaOuRegistro(identificador);
        if (porEmail.isPresent() && porMatricula.isPresent()
                && !porEmail.get().getId().equals(porMatricula.get().getId())) return Optional.empty();
        return porEmail.or(() -> porMatricula);
    }

    private boolean elegivel(Usuario usuario) {
        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo()) || usuario.getId() == null) return false;
        Perfil perfil = usuario.getPerfil();
        return perfil == Perfil.ALUNO || perfil == Perfil.SECRETARIA || perfil == Perfil.COORDENADOR;
    }
}
