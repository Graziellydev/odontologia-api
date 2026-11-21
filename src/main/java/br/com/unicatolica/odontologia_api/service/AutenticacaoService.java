package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponseDTO autenticar(LoginDTO dto) {
        if (dto == null || dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw credenciaisInvalidas();
        }

        String identificador = Optional.ofNullable(dto.getIdentificador())
                .filter(valor -> !valor.isBlank())
                .orElse(dto.getEmail());
        if (identificador == null || identificador.isBlank()) throw credenciaisInvalidas();

        Usuario usuario = resolverUsuario(identificador).orElseThrow(AutenticacaoService::credenciaisInvalidas);
        if (!Boolean.TRUE.equals(usuario.getAtivo())
                || !perfilPermitido(usuario)
                || !senhaValidaEAtualizarSeLegada(usuario, dto.getSenha())) {
            throw credenciaisInvalidas();
        }

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getMatriculaOuRegistro(),
                "Login realizado com sucesso como " + usuario.getPerfil());
    }

    private Optional<Usuario> resolverUsuario(String identificadorInformado) {
        String identificador = identificadorInformado.trim();
        Optional<Usuario> porEmail = usuarioRepository.findByEmailIgnoreCase(identificador.toLowerCase(java.util.Locale.ROOT));
        Optional<Usuario> porMatricula = usuarioRepository.findByMatriculaOuRegistro(identificador);
        if (porEmail.isPresent() && porMatricula.isPresent()
                && !porEmail.get().getId().equals(porMatricula.get().getId())) {
            return Optional.empty();
        }
        return porEmail.or(() -> porMatricula);
    }

    private boolean perfilPermitido(Usuario usuario) {
        return usuario.getPerfil() == br.com.unicatolica.odontologia_api.model.Perfil.ALUNO
                || usuario.getPerfil() == br.com.unicatolica.odontologia_api.model.Perfil.SECRETARIA
                || usuario.getPerfil() == br.com.unicatolica.odontologia_api.model.Perfil.COORDENADOR;
    }

    private boolean senhaValidaEAtualizarSeLegada(Usuario usuario, String senhaInformada) {
        String armazenada = usuario.getSenhaHash();
        if (armazenada == null || senhaInformada.isBlank()) return false;

        if (armazenada.startsWith("$2a$") || armazenada.startsWith("$2b$") || armazenada.startsWith("$2y$")) {
            try {
                return passwordEncoder.matches(senhaInformada, armazenada);
            } catch (IllegalArgumentException hashInvalido) {
                return false;
            }
        }

        boolean corresponde = MessageDigest.isEqual(
                senhaInformada.getBytes(StandardCharsets.UTF_8),
                armazenada.getBytes(StandardCharsets.UTF_8));
        if (corresponde) {
            usuario.setSenhaHash(passwordEncoder.encode(senhaInformada));
            usuarioRepository.save(usuario);
        }
        return corresponde;
    }

    private static BadCredentialsException credenciaisInvalidas() {
        return new BadCredentialsException("E-mail ou senha incorretos.");
    }
}
