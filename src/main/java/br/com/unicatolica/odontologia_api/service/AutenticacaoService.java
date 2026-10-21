package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Camada de serviço contendo a regra de negócio da autenticação do sistema.
 */
@Service
public class AutenticacaoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Valida as credenciais enviadas e realiza a autenticação do usuário.
     * 
     * @param dto Objeto contendo e-mail e senha informados
     * @return DTO com informações do usuário logado e mensagem de confirmação
     */
    public LoginResponseDTO autenticar(LoginDTO dto) {
        // 1. Busca o usuário pelo e-mail no PostgreSQL. Lança exceção se não encontrar.
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("E-mail ou senha incorretos!"));

        // 2. Compara a senha informada com a senha_hash salva no banco
        if (!usuario.getSenhaHash().equals(dto.getSenha())) {
            throw new RuntimeException("E-mail ou senha incorretos!");
        }

        // 3. Valida se a conta do usuário está ativa no sistema
        if (!usuario.getAtivo()) {
            throw new RuntimeException("Usuário inativo no sistema!");
        }

        // 4. Monta e retorna a resposta com o perfil e dados do usuário logado
        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getMatriculaOuRegistro(),
                "Login realizado com sucesso como " + usuario.getPerfil()
        );
    }
}
