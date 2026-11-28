package br.com.unicatolica.odontologia_api.repository;

import br.com.unicatolica.odontologia_api.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Modifying
    @Query("update PasswordResetToken t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    int invalidarNaoUsados(@Param("usuarioId") Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.usuarioId = :usuarioId and t.usado = false")
    Optional<PasswordResetToken> buscarAbertoParaAtualizacao(@Param("usuarioId") Long usuarioId);
}
