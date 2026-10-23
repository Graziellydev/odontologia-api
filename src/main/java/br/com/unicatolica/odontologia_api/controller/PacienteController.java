package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.model.Paciente;
import br.com.unicatolica.odontologia_api.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: Define que esta classe e um controller REST que devolve dados (JSON)
// @RequestMapping("/api/pacientes"): Define a rota/URL base para os endpoints de paciente
@RestController
@RequestMapping("/api/pacientes")
@CrossOrigin(origins = "*") // <-- Adicione esta linha para liberar o acesso do frontend
public class PacienteController {

    // Injecao do Service com as regras de negocio
    @Autowired
    private PacienteService pacienteService;

    // POST /api/pacientes
    // Endpoint utilizado pela Secretaria para cadastrar um novo paciente no sistema
    @PostMapping
    public ResponseEntity<?> cadastrarPaciente(@RequestBody Paciente paciente) {
        try {
            Paciente novoPaciente = pacienteService.salvar(paciente);
            // Retorna HTTP 201 (Created) com os dados do paciente salvo
            return ResponseEntity.status(HttpStatus.CREATED).body(novoPaciente);
        } catch (IllegalArgumentException e) {
            // Caso ocorra erro de validacao (ex: CPF duplicado), retorna HTTP 400 (Bad Request)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/pacientes
    // Retorna a lista completa de pacientes
    @GetMapping
    public ResponseEntity<List<Paciente>> listarTodos() {
        List<Paciente> pacientes = pacienteService.listarTodos();
        return ResponseEntity.ok(pacientes);
    }

    // GET /api/pacientes/{id}
    // Busca um paciente especifico pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return pacienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/pacientes/cpf/{cpf}
    // Busca um paciente especifico pelo CPF
    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<?> buscarPorCpf(@PathVariable String cpf) {
        return pacienteService.buscarPorCpf(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/pacientes/{id}
    // Atualiza os dados cadastrais de um paciente
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarPaciente(@PathVariable Long id, @RequestBody Paciente paciente) {
        try {
            Paciente pacienteAtualizado = pacienteService.atualizar(id, paciente);
            return ResponseEntity.ok(pacienteAtualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PATCH /api/pacientes/{id}/desligar
    // Altera a situacao do paciente para DESLIGADO (Exclusao logica)
    @PatchMapping("/{id}/desligar")
    public ResponseEntity<?> desligarPaciente(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam Long desligadoPorId) {
        try {
            Paciente pacienteDesligado = pacienteService.desligarPaciente(id, motivo, desligadoPorId);
            return ResponseEntity.ok(pacienteDesligado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
