# Proposal

## Why

O RF004 foi entregue como drawer contextual, mas o protótipo oficial define uma página clínica completa integrada a `Pacientes do turno` e ao encerramento do RF003. A especificação precisa substituir esse fluxo visual sem enfraquecer as regras server-side de rede, vínculo, turno, imutabilidade e auditoria já adotadas.

## What Changes

- Substituir a experiência de drawer/modal por uma rota ou estado de página contextual de prontuário, acessada pelo atendimento do turno e retornando por `← Voltar ao turno`.
- Manter e detalhar a autorização clínica por CIDR, vínculo aluno-paciente, data e turno: criação só para `AGENDADO`/`CONFIRMADA_24H`; leitura pós-registro e adendos também para `REALIZADO`; `COORDENADOR` somente em leitura.
- Consolidar o contrato de histórico para apresentar paciente, atendimento, evoluções imutáveis, adendos e odontograma sem expor identificadores pessoais além do necessário ao atendimento.
- Manter evolução e adendos append-only, o selo temporal SHA-256 e a atualização auditada de `consultas`; integrar o card de encerramento à sugestão de retorno ou à conclusão já fornecida pelo RF003.
- Reproduzir a composição medida do frame Figma `4:46292`: cabeçalho, faixa de ambiente, resumo clínico, abas, grid cronológico/formulário, estado assinado, encerramento e cenários de bloqueio.
- Aplicar os controles visuais RNF003: marca d'água repetida, seleção clínica desabilitada, bloqueio de impressão e esquema claro.

## Capabilities

### New Capabilities

- `student-clinical-access`: aplica autorização de prontuário por rede local, vínculo clínico, data, turno e perfil de auditoria.
- `patient-clinical-history`: fornece o histórico cronológico mínimo necessário para o atendimento, incluindo odontograma, evoluções e adendos.
- `immutable-clinical-records`: registra evolução e adendos imutáveis, selo temporal e transição auditada da consulta.
- `student-clinical-record-page`: apresenta o prontuário como página contextual fiel ao frame Figma e integra o encerramento do atendimento com as proteções visuais LGPD.

### Modified Capabilities

- Nenhuma. Não há especificações principais sincronizadas; a mudança anterior foi arquivada sem sincronização por descrever o drawer que esta proposta substitui.

## Impact

- Frontend: `frontend/src/PainelAluno/`, `frontend/src/Aluno/AplicacaoAluno.jsx`, `frontend/src/Retornos/EncerrarAtendimento.jsx` e os serviços de API passam a compor uma página clínica completa, preservando a navegação e componentes fora do RF004.
- Backend: o pacote `prontuarios/`, `SecurityConfig.java` e as consultas de atendimento do turno preservam os contratos existentes e alinham leitura, escrita e encerramento a uma mesma consulta.
- Banco: somente leitura/escrita em estruturas de V6, V8 e V9. Não haverá migration; mapeamentos devem continuar válidos com `spring.jpa.hibernate.ddl-auto=validate`, `UNIQUE (consulta_id)` e os triggers existentes.
- OpenSpec: a alteração anterior foi arquivada em `openspec/changes/archive/2026-11-20-rf004-registro-informacoes-prontuario-aluno` sem sincronizar suas especificações de drawer, evitando um contrato concorrente.
