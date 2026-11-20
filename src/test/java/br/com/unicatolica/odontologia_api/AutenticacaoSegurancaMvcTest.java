package br.com.unicatolica.odontologia_api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static br.com.unicatolica.odontologia_api.CsrfTestSupport.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.unicatolica.odontologia_api.config.SecurityConfig;
import br.com.unicatolica.odontologia_api.controller.AutenticacaoController;
import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AutenticacaoController.class)
@Import(SecurityConfig.class)
class AutenticacaoSegurancaMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacaoService;
    @MockitoBean private UsuarioRepository usuarioRepository;

    @Test
    void loginInvalidoRetornaMensagemGenericaSemDistinguirEmailDeSenha() throws Exception {
        when(autenticacaoService.autenticar(any(LoginDTO.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        String porEmail = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"inexistente@teste.invalid\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String porSenha = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"existente@teste.invalid\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals("E-mail ou senha incorretos.", porEmail);
        org.junit.jupiter.api.Assertions.assertEquals(porEmail, porSenha);
    }
}
