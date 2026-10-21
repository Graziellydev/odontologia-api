package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST responsável por expor os endpoints de autenticação.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Permite que o frontend conecte na API
public class AutenticacaoController {

    @Autowired
    private AutenticacaoService autenticacaoService;

    /**
     * Endpoint HTTP POST para autenticar usuários.
     * URL: POST http://localhost:8080/api/auth/login
     * 
     * @param dto Dados de login (email e senha) no corpo JSON da requisição
     * @return Resposta HTTP 200 OK com os dados do perfil logado ou 401 Não Autorizado
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        try {
            LoginResponseDTO resposta = autenticacaoService.autenticar(dto);
            return ResponseEntity.ok(resposta);
        } catch (RuntimeException e) {
            // Captura o erro gerado pelo Service e devolve exatamente o Status 401 que o React espera
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha incorretos.");
        }
    }
}