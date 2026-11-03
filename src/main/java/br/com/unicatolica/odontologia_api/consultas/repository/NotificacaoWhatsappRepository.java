package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.entity.NotificacaoWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoWhatsappRepository extends JpaRepository<NotificacaoWhatsapp, Long> { }
