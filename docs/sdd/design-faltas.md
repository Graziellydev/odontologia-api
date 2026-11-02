# DESIGN: Gestão de Faltas

## 1. Mapeamento de Entidades (Baseado no V5)
* `Consulta`: Core do módulo. Controla `status_consulta` e `vaga_id`.
* `Vaga`: Representa a Data, Turno e Cadeira.
* `ConsultaHistorico`: Auditoria preenchida por Trigger (V8). O Back-end não precisa inserir manualmente, o BD assume.

## 2. Contratos da API (JSON)

### POST /api/consultas/{id}/faltas
**Descrição:** Registra a ausência.
**Request Body:**
{
  "quemFaltou": "PACIENTE", // Enum: PACIENTE, ALUNO
  "justificada": true,
  "justificativa": "Paciente relatou problema com transporte do interior."
}
**Response:** 200 OK

### POST /api/consultas/{id}/reagendamentos
**Descrição:** Cria a nova consulta a partir da falta.
**Request Body:**
{
  "novaVagaId": 142, // A vaga já validada com Data, Turno e Cadeira
  "notificarWhatsapp": true
}
**Response:** 201 Created (retorna o ID da nova consulta)

### GET /api/consultas/faltas?mes=11&quemFaltou=PACIENTE
**Descrição:** Retorna os dados para a tabela de ausências.
**Response (Array):**
[{
  "consultaId": 45,
  "data": "2026-11-05",
  "ausencia": "Paciente", // Mapeado do status_consulta
  "vinculoPaciente": "Maria Silva",
  "vinculoAluno": "João",
  "situacao": "Pendente",
  "proximoPasso": "Reagendar"
}]