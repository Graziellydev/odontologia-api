package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.dto.AnaliseSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.dto.CandidatoFilaDTO;
import br.com.unicatolica.odontologia_api.dto.SolicitarPacienteDTO;
import br.com.unicatolica.odontologia_api.dto.VagaDisponivelDTO;
import br.com.unicatolica.odontologia_api.dto.VincularPacienteDTO;
import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.SolicitarPaciente;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.SolicitarPacienteRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SolicitarPacienteService {
    private final SolicitarPacienteRepository repository;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public SolicitarPacienteService(SolicitarPacienteRepository repository,
                                   UsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    private void exigirPerfil(Long id, Perfil perfil) {
        if (id == null) throw erro(HttpStatus.BAD_REQUEST, "Informe o ID do usuário");
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST, "Usuário não encontrado"));
        if (!Boolean.TRUE.equals(u.getAtivo()) || u.getPerfil() != perfil) {
            throw erro(HttpStatus.FORBIDDEN, "Usuário precisa estar ativo e ter perfil " + perfil);
        }
    }

    // Cadastro: só aceita aluno matriculado em turma ativa e procedimento permitido
    // para a disciplina, dente e classe. O banco ainda faz a mesma validação por trigger.
    @Transactional
    public SolicitarPaciente solicitar(SolicitarPacienteDTO dto) {
        exigirPerfil(dto.alunoId(), Perfil.ALUNO);

        Integer matriculas = jdbc.queryForObject("""
            SELECT COUNT(*) FROM matriculas_turma m
            JOIN turmas t ON t.id = m.turma_id AND t.ativa = TRUE
            JOIN disciplinas d ON d.id = t.disciplina_id AND d.ativo = TRUE
            WHERE m.aluno_id = ? AND m.turma_id = ?
            """, Integer.class, dto.alunoId(), dto.turmaId());
        if (matriculas == null || matriculas == 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Aluno não matriculado em turma ativa");
        }

        List<Boolean> exigeClasse = jdbc.query("""
            SELECT exige_classe FROM procedimentos WHERE id = ? AND ativo = TRUE
            """, (rs, i) -> rs.getBoolean(1), dto.procedimentoId());
        if (exigeClasse.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Procedimento inexistente ou inativo");
        }
        if (exigeClasse.get(0) && (dto.classe() == null || dto.classe().isBlank())) {
            throw erro(HttpStatus.BAD_REQUEST, "A classe é obrigatória para esse procedimento");
        }
        if (dto.dataDesejada() != null && dto.dataDesejada().isBefore(LocalDate.now())) {
            throw erro(HttpStatus.BAD_REQUEST, "A data desejada não pode estar no passado");
        }

        Boolean permitido = jdbc.queryForObject("""
            SELECT fn_procedimento_permitido(t.disciplina_id, ?, ?, ?)
            FROM turmas t WHERE t.id = ?
            """, Boolean.class, dto.procedimentoId(), dto.dente(), dto.classe(), dto.turmaId());
        if (!Boolean.TRUE.equals(permitido)) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "O procedimento/dente/classe não é permitido para esta disciplina");
        }

        SolicitarPaciente s = new SolicitarPaciente();
        s.setAlunoId(dto.alunoId());
        s.setTurmaId(dto.turmaId());
        s.setProcedimentoId(dto.procedimentoId());
        s.setDente(dto.dente());
        s.setClasse(dto.classe());
        s.setDataDesejada(dto.dataDesejada());
        String notaAluno = dto.observacaoSecretaria();
        s.setObservacaoSecretaria(
                notaAluno == null || notaAluno.isBlank()
                        ? null
                        : notaAluno.trim()
        );
        s.setStatusSolicitacao("PENDENTE_APROVACAO");
        return repository.saveAndFlush(s);
    }

    public List<SolicitarPaciente> listarTodas() { return repository.findAll(); }

    public List<SolicitarPaciente> listarPendentes() {
        return repository.findByStatusSolicitacaoOrderByDataSolicitacaoAsc("PENDENTE_APROVACAO");
    }

    public List<SolicitarPaciente> listarPorAluno(Long alunoId) {
        return repository.findByAlunoIdOrderByDataSolicitacaoDesc(alunoId);
    }

    // Usado somente dentro de métodos @Transactional: bloqueia análise simultânea.
    private SolicitarPaciente buscarParaAtualizar(Long id) {
        return repository.buscarParaAtualizar(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
    }

    private void exigirPendente(SolicitarPaciente s) {
        if (!"PENDENTE_APROVACAO".equals(s.getStatusSolicitacao())) {
            throw erro(HttpStatus.CONFLICT, "Esta solicitação já foi analisada");
        }
    }

    private void registrarAnalise(SolicitarPaciente s, Long secretariaId, String observacao) {
        exigirPerfil(secretariaId, Perfil.SECRETARIA);
        s.setSecretariaAnalisadorId(secretariaId);
        s.setDataAnalise(OffsetDateTime.now());
        s.setRespostaSecretaria(observacao);
    }

    // Primeiro a secretaria autoriza o pedido, mas ele continua esperando paciente/vaga.
    @Transactional
    public SolicitarPaciente aprovar(Long id, AnaliseSolicitacaoDTO dto) {
        SolicitarPaciente s = buscarParaAtualizar(id);
        exigirPendente(s);
        registrarAnalise(s, dto.secretariaId(), dto.observacao());
        s.setStatusSolicitacao("PENDENTE_FILA");
        return repository.saveAndFlush(s);
    }

    @Transactional
    public SolicitarPaciente recusar(Long id, AnaliseSolicitacaoDTO dto) {
        if (dto.observacao() == null || dto.observacao().isBlank()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a justificativa da recusa");
        }
        SolicitarPaciente s = buscarParaAtualizar(id);
        exigirPendente(s);
        registrarAnalise(s, dto.secretariaId(), dto.observacao());
        s.setStatusSolicitacao("RECUSADO");
        return repository.saveAndFlush(s);
    }

    // Só aceita correspondência CLÍNICA confirmada: procedimento e dente devem
    // ser iguais; classe também quando solicitada. Ausência de dente/classe na
    // fila NÃO é tratada como confirmação de compatibilidade.
    public List<CandidatoFilaDTO> candidatos(Long id) {
        SolicitarPaciente s = repository.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        return jdbc.query("""
            SELECT f.id, f.paciente_id, p.nome_completo, f.nivel_urgencia,
                   f.data_entrada_fila, f.dente, f.classe
            FROM fila_espera f
            JOIN pacientes p ON p.id = f.paciente_id AND p.situacao = 'ATIVO'
            WHERE f.status_fila = 'AGUARDANDO'
              AND f.procedimento_id = ?
              AND f.dente = ?
              AND (CAST(? AS VARCHAR) IS NULL OR f.classe = ?)
            ORDER BY CASE f.nivel_urgencia
                WHEN 'PNE' THEN 0 WHEN 'ALTA' THEN 1
                WHEN 'MEDIA' THEN 2 ELSE 3 END,
                f.prioridade_reagendamento DESC, f.data_entrada_fila ASC
            """, (rs, i) -> new CandidatoFilaDTO(
                    rs.getLong(1), rs.getLong(2), rs.getString(3),
                    rs.getString(4), rs.getObject(5, OffsetDateTime.class),
                    rs.getString(6), rs.getString(7)),
                s.getProcedimentoId(), s.getDente(), s.getClasse(), s.getClasse());
    }

    // Consulta a view criada em V8, sem incluir vagas bloqueadas ou ocupadas.
    public List<VagaDisponivelDTO> vagasDisponiveis(LocalDate data) {
        String sql = """
            SELECT vaga_id, data, turno, cadeira
            FROM v_vagas_disponiveis
            WHERE data >= CURRENT_DATE
            """ + (data != null ? " AND data = ? " : " ")
                  + " ORDER BY data, turno, cadeira LIMIT 100";
        if (data == null) {
            return jdbc.query(sql, (rs, i) -> new VagaDisponivelDTO(
                    rs.getLong("vaga_id"), rs.getDate("data").toLocalDate(),
                    rs.getString("turno"), rs.getString("cadeira")));
        }
        return jdbc.query(sql, (rs, i) -> new VagaDisponivelDTO(
                rs.getLong("vaga_id"), rs.getDate("data").toLocalDate(),
                rs.getString("turno"), rs.getString("cadeira")), Date.valueOf(data));
    }

    // Vincular + agendar na MESMA transação. Se falhar, tudo volta ao estado anterior.
    @Transactional
    public SolicitarPaciente vincular(Long id, VincularPacienteDTO dto) {
        SolicitarPaciente s = buscarParaAtualizar(id);
        if (!"PENDENTE_FILA".equals(s.getStatusSolicitacao())) {
            throw erro(HttpStatus.CONFLICT, "A solicitação precisa estar aprovada e aguardando paciente");
        }
        exigirPerfil(dto.secretariaId(), Perfil.SECRETARIA);

        // Trava a vaga para impedir que duas operações desta API a reservem juntas.
        List<LocalDate> datas = jdbc.query("""
            SELECT v.data FROM vagas v
            JOIN cadeiras ca ON ca.id = v.cadeira_id AND ca.ativa = TRUE
            WHERE v.id = ? AND v.bloqueada = FALSE AND v.data >= CURRENT_DATE
            FOR UPDATE OF v
            """, (rs, i) -> rs.getDate(1).toLocalDate(), dto.vagaId());
        if (datas.isEmpty()) throw erro(HttpStatus.CONFLICT, "Vaga inexistente, bloqueada ou passada");

        Integer ocupacoes = jdbc.queryForObject("""
            SELECT COUNT(*) FROM consultas c
            WHERE c.vaga_id = ?
              AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
            """, Integer.class, dto.vagaId());
        if (ocupacoes != null && ocupacoes > 0) {
            throw erro(HttpStatus.CONFLICT, "Vaga já ocupada");
        }

        List<Long> pacientes = jdbc.query("""
            SELECT f.paciente_id
            FROM fila_espera f
            JOIN pacientes p ON p.id = f.paciente_id AND p.situacao = 'ATIVO'
            WHERE f.id = ? AND f.status_fila = 'AGUARDANDO'
              AND f.procedimento_id = ? AND f.dente = ?
              AND (CAST(? AS VARCHAR) IS NULL OR f.classe = ?)
            FOR UPDATE OF f
            """, (rs, i) -> rs.getLong(1), dto.filaEsperaId(),
                s.getProcedimentoId(), s.getDente(), s.getClasse(), s.getClasse());
        if (pacientes.isEmpty()) {
            throw erro(HttpStatus.CONFLICT, "Paciente incompatível ou não disponível na fila");
        }

        // Garante que paciente e aluno não recebam dois atendimentos no mesmo turno.
        Integer conflitos = jdbc.queryForObject("""
            SELECT COUNT(*) FROM consultas c
            JOIN vagas v ON v.id = c.vaga_id
            JOIN vagas nova ON nova.id = ?
            WHERE v.data = nova.data AND v.turno = nova.turno
              AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
              AND (c.paciente_id = ? OR c.aluno_id = ?)
            """, Integer.class, dto.vagaId(), pacientes.get(0), s.getAlunoId());
        if (conflitos != null && conflitos > 0) {
            throw erro(HttpStatus.CONFLICT, "Aluno ou paciente já possui consulta nesse turno");
        }

        // O trigger de V8 registra a secretaria e a origem no histórico.
        jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)",
                String.class, dto.secretariaId().toString());
        jdbc.queryForObject("SELECT set_config('app.origem', 'SECRETARIA', true)", String.class);

        jdbc.update("""
            INSERT INTO consultas (
                paciente_id, aluno_id, turma_id, procedimento_id,
                dente, classe, vaga_id, fila_espera_id, criado_por_id,
                status_consulta
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AGENDADO')
            """, pacientes.get(0), s.getAlunoId(), s.getTurmaId(),
                s.getProcedimentoId(), s.getDente(), s.getClasse(),
                dto.vagaId(), dto.filaEsperaId(), dto.secretariaId());

        int alteradas = jdbc.update("""
            UPDATE fila_espera
            SET status_fila = 'ALOCADO', data_saida_fila = now(), versao = versao + 1
            WHERE id = ? AND status_fila = 'AGUARDANDO'
            """, dto.filaEsperaId());
        if (alteradas != 1) throw erro(HttpStatus.CONFLICT, "Paciente já alocado");

        s.setFilaEsperaId(dto.filaEsperaId());
        s.setPacienteAlocadoId(pacientes.get(0));
        s.setStatusSolicitacao("APROVADO_E_ALOCADO");
        return repository.saveAndFlush(s);
    }
}
