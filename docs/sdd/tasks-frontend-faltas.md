# TASKS: Front-end - Gestão de Faltas

1. **Serviço de API (`frontend/src/services/FaltasService.js`):**
   * Criar funções utilizando `axios` (ou `fetch`) para os endpoints:
     - `registrarFalta(consultaId, dados)` -> `POST /api/consultas/{id}/faltas`
     - `reagendarFalta(origemId, dados)` -> `POST /api/consultas/{id}/reagendamentos`
     - `listarFaltas()` -> `GET /api/consultas/faltas`

2. **Estilização (`frontend/src/Secretaria/AgendaSecretaria.css`):**
   * Converter o código bruto do arquivo de texto do Figma em classes CSS semânticas e modulares (ex: `.painel`, `.btn-primario`, `.tabela-ausencias`).

3. **Componentes React (`frontend/src/Secretaria/`):**
   * `AgendaSecretaria.jsx`: Componente container. Gerencia o estado global da tela (lista de faltas, consulta selecionada para reagendamento) e repassa via *props*.
   * `PainelRegistroFalta.jsx`: Formulário com os campos "ID", "Quem Faltou" (select) e "Observação" (textarea).
   * `PainelReagendamento.jsx`: Formulário com os campos "Nova Vaga ID" e checkbox "Notificar WhatsApp". Recebe os dados da consulta original via props.
   * `TabelaHistoricoAusencias.jsx`: Renderiza os `<tr>` baseados no array de faltas retornado pelo backend. Inclui um botão na última coluna para disparar a seleção.