package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock private UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AutenticacaoService service;

    @BeforeEach void setup() { service = new AutenticacaoService(usuarios, encoder); }

    @Test void aceitaSenhaBcryptSemExporHashNaResposta() {
        Usuario usuario = usuario(encoder.encode("Senha-sintetica-123"));
        when(usuarios.findByEmailIgnoreCase("secretaria@teste.invalid")).thenReturn(Optional.of(usuario));

        var resposta = service.autenticar(login("Senha-sintetica-123"));

        assertEquals("secretaria@teste.invalid", resposta.getEmail());
        assertFalse(resposta.toString().contains(usuario.getSenhaHash()));
        verify(usuarios, never()).save(any());
    }

    @Test void aceitaLoginDaSecretariaPorMatricula() {
        Usuario usuario = usuario(encoder.encode("Senha-sintetica-123"));
        usuario.setMatriculaOuRegistro("SEC-001");
        LoginDTO dto = new LoginDTO();
        dto.setIdentificador(" sec-001 ");
        dto.setSenha("Senha-sintetica-123");
        when(usuarios.findByEmailIgnoreCase("sec-001")).thenReturn(Optional.empty());
        when(usuarios.findByMatriculaOuRegistro("sec-001")).thenReturn(Optional.of(usuario));

        var resposta = service.autenticar(dto);

        assertEquals(Perfil.SECRETARIA, resposta.getPerfil());
        assertEquals("SEC-001", resposta.getMatriculaOuRegistro());
    }

    @Test void atualizaSenhaLegadaParaBcryptNoLoginValido() {
        Usuario usuario = usuario("Senha-legada-sintetica");
        when(usuarios.findByEmailIgnoreCase("secretaria@teste.invalid")).thenReturn(Optional.of(usuario));

        service.autenticar(login("Senha-legada-sintetica"));

        assertTrue(usuario.getSenhaHash().startsWith("$2"));
        assertTrue(encoder.matches("Senha-legada-sintetica", usuario.getSenhaHash()));
        verify(usuarios).save(usuario);
    }

    @Test void rejeitaSenhaInvalidaEUsuarioInativoComErroGenerico() {
        Usuario usuario = usuario(encoder.encode("senha-correta"));
        when(usuarios.findByEmailIgnoreCase("secretaria@teste.invalid")).thenReturn(Optional.of(usuario));
        assertThrows(BadCredentialsException.class, () -> service.autenticar(login("senha-errada")));

        usuario.setAtivo(false);
        assertThrows(BadCredentialsException.class, () -> service.autenticar(login("senha-correta")));
    }

    private static LoginDTO login(String senha) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("secretaria@teste.invalid");
        dto.setSenha(senha);
        return dto;
    }

    private static Usuario usuario(String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setNome("Secretaria Teste");
        usuario.setEmail("secretaria@teste.invalid");
        usuario.setSenhaHash(senhaHash);
        usuario.setPerfil(Perfil.SECRETARIA);
        usuario.setAtivo(true);
        return usuario;
    }
}
