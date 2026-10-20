package br.com.unicatolica.odontologia_api.filaespera.service;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilaEsperaService {

    private final FilaEsperaRepository filaEsperaRepository;

    public FilaEsperaService(FilaEsperaRepository filaEsperaRepository) {
        this.filaEsperaRepository = filaEsperaRepository;
    }

    public List<FilaEspera> listarFila() {
        return filaEsperaRepository.findAllByOrderByDataEntradaFilaAsc();
    }

    public List<FilaEspera> listarPorStatus(String statusFila) {
        return filaEsperaRepository.findByStatusFilaOrderByDataEntradaFilaAsc(statusFila);
    }
}