package br.com.unicatolica.odontologia_api.prontuarios.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProntuarioHistoricoResponse(
        PacienteResumo paciente,
        AnamneseResumo anamnese,
        List<OdontogramaItem> odontograma,
        ConsultaResumo consultaAtiva,
        List<EvolucaoItem> evolucoes) {

    public record PacienteResumo(Long id, String nome, String numeroProntuario, Integer idade) { }

    public record AnamneseResumo(
            String queixaPrincipal,
            String historiaQueixa,
            Map<String, Object> respostasQuestionario,
            String pressaoArterial,
            Integer pulsoBpm,
            java.math.BigDecimal temperaturaC,
            Integer frequenciaRespiratoria,
            Integer medoDentistaEscala,
            Integer frequenciaEscovacao,
            Boolean usaFioDental,
            Boolean usaEnxaguante,
            Boolean limpaLingua) { }

    public record OdontogramaItem(Long id, String dente, String faceDente, String condicaoDente,
                                  String observacao, OffsetDateTime atualizadoEm) { }

    public record ConsultaResumo(Long consultaId, Long turmaId, Long procedimentoId,
                                 String procedimento, String dente, String classe,
                                 LocalDate data, String turno, String status) { }

    public record EvolucaoItem(Long id, Long consultaId, Long procedimentoId, String procedimento,
                               String dente, String classe, String evolucaoClinica, String aluno,
                               String assinaturaDigital, OffsetDateTime dataRegistro,
                               List<AdendoItem> adendos) { }

    public record AdendoItem(Long id, Long autorId, String autor, String texto, OffsetDateTime criadoEm) { }
}
