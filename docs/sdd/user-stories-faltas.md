# Histórias de Usuário: Gestão de Faltas e Reagendamento

## US01 - Registro de Ausência
**Como** secretária da clínica,
**Quero** poder registrar a falta de um paciente ou de um aluno em uma consulta agendada,
**Para que** a vaga ociosa seja contabilizada e o prontuário seja devidamente atualizado no sistema.

* **Critérios de Aceitação:**
  * O sistema só deve permitir registrar falta em consultas com status `AGENDADO` ou `CONFIRMADA_24H`.
  * Deve ser obrigatório informar quem faltou (`PACIENTE` ou `ALUNO`).
  * Deve permitir inserir uma justificativa em texto.
  * O status da consulta deve ser alterado para `FALTOU_PACIENTE` ou `FALTOU_ALUNO`.

---

## US02 - Reagendamento Exclusivo para Faltas
**Como** secretária da clínica,
**Quero** reagendar uma consulta a partir de um registro de falta anterior,
**Para que** o paciente não perca o tratamento e o histórico da falta seja mantido para auditoria.

* **Critérios de Aceitação:**
  * A opção de reagendamento só deve ser habilitada se a consulta original estiver com status de falta.
  * O sistema deve criar um **novo** registro de consulta consumindo uma nova vaga (Cadeira, Data, Turno).
  * O novo registro deve manter o vínculo com a consulta original (`consulta_origem_id`).
  * A consulta original (a que ocorreu a falta) deve permanecer no banco de dados inalterada, mantendo seu status.

---

## US03 - Histórico de Ausências
**Como** secretária da clínica,
**Quero** visualizar uma tabela com o histórico de consultas que tiveram falta,
**Para que** eu possa entrar em contato com os pacientes e realizar o reagendamento ativamente.

* **Critérios de Aceitação:**
  * A tabela deve exibir: Data, Quem faltou (Ausência), Vínculo (Nome), Situação e Ação.
  * Deve listar apenas consultas que possuam os status `FALTOU_PACIENTE` ou `FALTOU_ALUNO`.