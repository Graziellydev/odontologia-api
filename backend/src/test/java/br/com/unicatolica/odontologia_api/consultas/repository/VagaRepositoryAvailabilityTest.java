package br.com.unicatolica.odontologia_api.consultas.repository;

import org.springframework.data.jpa.repository.Query;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VagaRepositoryAvailabilityTest {

    @Test void reservaSomenteCadeiraAtivaComVagaDesbloqueada() throws Exception {
        Query query = VagaRepository.class
                .getMethod("reservarPrimeiraDisponivel", LocalDate.class, String.class)
                .getAnnotation(Query.class);

        assertTrue(query.nativeQuery());
        assertTrue(query.value().contains("ca.ativa = TRUE"));
        assertTrue(query.value().contains("NOT v.bloqueada"));
        assertTrue(query.value().contains("NOT EXISTS"));
    }
}
