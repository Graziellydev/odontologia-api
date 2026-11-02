# Documento de Requisitos: Gestão de Faltas e Reagendamento

## Requisitos Funcionais (RF)
* **RF01:** O sistema deve permitir o registro de faltas, atualizando o status da consulta para `FALTOU_PACIENTE` ou `FALTOU_ALUNO`.
* **RF02:** O sistema deve exigir a indicação de quem foi o responsável pela falta na consulta.
* **RF03:** O sistema deve permitir o armazenamento de uma justificativa textual para a falta (`justificativa_falta`) e marcar se foi justificada (`falta_justificada`).
* **RF04:** O sistema deve bloquear qualquer tentativa de reagendamento de uma consulta que não possua um status prévio de falta (Proteção de Rota).
* **RF05:** O processo de reagendamento deve gerar uma nova entidade de Consulta no banco de dados, associada a uma nova `vaga_id`.
* **RF06:** O sistema deve vincular a nova consulta reagendada à consulta original através do campo `consulta_origem_id`.
* **RF07:** O sistema deve listar todas as ausências registradas para consumo da interface gráfica (tabela do Figma).

## Requisitos Não Funcionais (RNF)
* **RNF01 (Arquitetura):** O código deve ser implementado em Java com Spring Boot, seguindo o padrão de projeto Package by Feature no pacote `consultas`.
* **RNF02 (Persistência):** O sistema deve utilizar Spring Data JPA e mapear a tabela `consultas` e `vagas` já existentes no PostgreSQL.
* **RNF03 (Contrato de Interface):** A comunicação com o frontend deve ser feita via endpoints REST consumindo e produzindo `application/json` através de DTOs.
* **RNF04 (Segurança de Dados):** O sistema nunca deve excluir o registro da consulta onde ocorreu a falta (Soft Tracking), garantindo auditoria pelo histórico.