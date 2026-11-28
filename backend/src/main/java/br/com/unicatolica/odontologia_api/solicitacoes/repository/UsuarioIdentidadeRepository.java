package br.com.unicatolica.odontologia_api.solicitacoes.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioIdentidadeRepository {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioIdentidadeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> buscarIdPorEmail(String email) {

        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM usuarios WHERE email = ?",
                (resultado, linha) -> resultado.getLong("id"),
                email);

        return ids.stream().findFirst();
    }
}