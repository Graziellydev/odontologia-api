# SPEC: Gestão de Faltas e Reagendamento

## 1. Objetivo
Fornecer à secretaria uma interface para registrar o não comparecimento em consultas agendadas, identificar se a falta partiu do Paciente ou do Aluno, e garantir um fluxo seguro de reagendamento exclusivo para esses casos.

## 2. Contexto
A recepção da clínica da UniCatólica precisa atualizar as consultas ao fim de cada turno. Quando uma ausência ocorre, a vaga é desperdiçada. Registrar corretamente o status `FALTOU_PACIENTE` ou `FALTOU_ALUNO` permite rastreabilidade (via `consulta_historico`) e libera o paciente/aluno para uma repescagem (reagendamento).

## 3. Requisitos de Negócio e Restrições
* **REQ-01 (Registro):** A falta só pode ser registrada se o `status_consulta` atual for `AGENDADO` ou `CONFIRMADA_24H`.
* **REQ-02 (Quem faltou):** O sistema deve registrar a falta alterando o `status_consulta` para `FALTOU_PACIENTE` ou `FALTOU_ALUNO`, preenchendo os campos `falta_justificada` e `justificativa_falta`.
* **REQ-03 (Bloqueio de Reagendamento):** O fluxo de reagendamento só aceitará `consulta_origem_id` caso a consulta original possua o status de falta.
* **REQ-04 (Novo Vínculo):** Reagendar cria um **novo** registro em `consultas`, consumindo uma nova `vaga_id` (que cruza Cadeira + Data + Turno), vinculando à `consulta_origem_id`.
* **REQ-05 (Histórico):** A tabela de histórico na tela exibirá consultas filtradas por `status_consulta IN ('FALTOU_PACIENTE', 'FALTOU_ALUNO')`.

## 4. Critérios de Aceitação
* [ ] **Dado** uma consulta agendada, **Quando** a secretária registra a falta do paciente com justificativa, **Então** o status da consulta muda para `FALTOU_PACIENTE` e o banco salva a `justificativa_falta`.
* [ ] **Dado** uma tentativa de reagendar uma consulta com status `REALIZADO`, **Quando** a requisição for enviada, **Então** a API deve retornar erro 400 (Bad Request) bloqueando a ação.
* [ ] **Dado** uma consulta com status `FALTOU_PACIENTE`, **Quando** reagendada com sucesso, **Então** uma nova consulta é gerada associada a uma nova `vaga_id` e a antiga permanece intacta para auditoria.