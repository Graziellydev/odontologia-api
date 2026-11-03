# TASKS: Backend - Gestão de Faltas

1. **Pacote Core:** Criar o pacote `br.com.unicatolica.odontologia_api.consultas`.
2. **Entidades JPA:** 
   * Mapear `@Entity Consulta` (garantindo os `@Enumerated` para o `StatusConsulta` que inclui `FALTOU_PACIENTE` e `FALTOU_ALUNO`).
   * Mapear `@Entity Vaga` e `@Entity Cadeira`.
3. **DTOs:** 
   * Criar `FaltaRequestDTO` em `consultas.dto`.
   * Criar `ReagendamentoRequestDTO` em `consultas.dto`.
   * Criar `HistoricoAusenciaResponseDTO` em `consultas.dto`.
4. **Repository:** 
   * `ConsultaRepository`: Criar query customizada `List<Consulta> findByStatusConsultaIn(...)`.
5. **Service (`ConsultaService`):**
   * Implementar `registrarFalta(Long consultaId, FaltaRequestDTO dto)`. Lançar exceção se status atual não for AGENDADO/CONFIRMADA_24H.
   * Implementar `reagendarFalta(Long consultaOrigemId, ReagendamentoRequestDTO dto)`. Validar se origem é falta. Se sim, clonar dados básicos e associar nova `vaga_id`.
6. **Controller (`ConsultaController` ou `FaltasController`):**
   * Mapear os 3 endpoints listados no Design.