package br.com.unicatolica.odontologia_api.repository;

import br.com.unicatolica.odontologia_api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Interface Repository para a tabela 'usuarios'.
 * Herda do JpaRepository para fornecer operações CRUD e buscas automáticas no banco.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário no PostgreSQL pelo e-mail informado.
     * O Spring Data JPA gera o SQL automático: SELECT * FROM usuarios WHERE email = ?
     */
    Optional<Usuario> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarPorIdParaAtualizacao(@Param("id") Long id);
}
