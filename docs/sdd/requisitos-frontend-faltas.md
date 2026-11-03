# Requisitos de Front-end: Gestão de Faltas (React)

## 1. Visão Geral
A interface da Secretaria deve permitir o registro rápido de faltas e o reagendamento imediato de consultas perdidas, consumindo os endpoints REST do back-end em Spring Boot.

## 2. Requisitos de Interface (UI/UX)
* **RF-FE-01:** O layout deve seguir estritamente as diretrizes do CSS exportado do Figma (tipografia Lato, background `#F7F8FA`, painéis com border-radius `12px`).
* **RF-FE-02:** O sistema de cores deve refletir as ações: Laranja (`#C65117`) para botões principais, Verde (`#287252` e fundo `#EAF5EE`) para estados confirmados, e Vermelho (`#B43B39` e fundo `#FFF0EF`) para faltas.
* **RF-FE-03:** A tela deve ser dividida em duas áreas superiores (Painel de Registro e Painel de Reagendamento) e uma área inferior (Tabela de Histórico).

## 3. Requisitos de Comportamento (Lógica React)
* **RF-FE-04 (Registro):** O `PainelRegistroFalta` deve ter um formulário para informar o ID da Consulta, selecionar quem faltou (Paciente/Aluno) e inserir uma justificativa.
* **RF-FE-05 (Reagendamento Condicional):** O `PainelReagendamento` deve ficar bloqueado/vazio por padrão. Ele só deve ser ativado se o usuário clicar na ação "Reagendar" em uma linha específica da Tabela de Histórico.
* **RF-FE-06 (Listagem):** A `TabelaHistoricoAusencias` deve consumir o endpoint `GET /api/consultas/faltas` assim que a página for montada (`useEffect`) e atualizar automaticamente sempre que uma nova falta ou reagendamento for registrado.