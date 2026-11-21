# Proposal

## Why

O módulo do Aluno permite acompanhar metas, solicitar pacientes e encerrar atendimentos, mas ainda não oferece acesso controlado ao prontuário nem registro da evolução clínica. Esta mudança habilita a consulta do histórico do paciente e a criação de registros assinados e imutáveis durante o atendimento, com controles de rede, vínculo, turno e proteção visual de dados clínicos.

## What Changes

- Adicionar validação de rede local e de consulta ativa do paciente pelo aluno autenticado, com restrição por data e turno e configuração para testes fora do horário comercial.
- Expor histórico clínico do paciente, incluindo dados resumidos de anamnese, odontograma, evoluções anteriores, adendos e consulta ativa, para aluno autorizado e coordenador em auditoria.
- Permitir salvar uma evolução por consulta, gerar um selo temporal baseado em hash SHA-256, concluir a consulta opcionalmente e registrar adendos sem alterar o prontuário assinado.
- Integrar ao Painel do Aluno um drawer/modal amplo com abas de evolução e histórico, modal de adendo e alerta compacto para bloqueios de acesso, seguindo o fluxo visual fornecido.
- Aplicar marca d’água de sessão, seleção de texto desabilitada na área clínica e ocultação na impressão.
- Manter o schema das tabelas existentes: `prontuarios.consulta_id` é único; `prontuarios` e `prontuario_adendos` aceitam apenas inserções depois do registro; o odontograma identifica cada dente/face pela combinação única já existente.

## Capabilities

### New Capabilities

- `student-clinical-access`: controla acesso por IP local, vínculo do aluno com paciente e turno da consulta (RNF001 e RNF002).
- `patient-clinical-history`: consulta e apresenta dados clínicos resumidos e histórico cronológico (HU07).
- `immutable-clinical-records`: cria evoluções assinadas e adendos imutáveis, com conclusão opcional da consulta (HU06).
- `student-clinical-record-ui`: integra o fluxo visual de prontuário e aplica proteção contra exposição casual na interface (RNF003).

### Modified Capabilities

Nenhuma. O inventário atual de especificações OpenSpec está vazio.

## Impact

- Backend: novo pacote de prontuários integrado às entidades e repositórios clínicos e de consultas existentes; regras REST e autorização em `SecurityConfig.java`; configuração de CIDRs e turno em `application.properties`.
- Frontend: extensão de `frontend/src/PainelAluno/` e integração contextual em `frontend/src/Aluno/AplicacaoAluno.jsx`, reutilizando `frontend/src/api.js`.
- Banco: leitura e escrita nas tabelas criadas por V6, V8 e V9. Não requer alteração de colunas nem constraints. A atualização do status da consulta deve configurar `app.origem='ALUNO'` e `app.usuario_id` na mesma transação para o trigger de histórico.
- Auditoria: o banco e `Perfil.java` não aceitam `ADMINISTRADOR`; por decisão de escopo, acesso de auditoria será autorizado ao perfil existente `COORDENADOR`.
- Assinatura: o campo existente `assinatura_digital TEXT` armazenará o resumo SHA-256 versionado da representação clínica canônica, timestamp ISO-8601 e matrícula/identificação do aluno. Esse hash e o timestamp são um selo de integridade e autoria da aplicação, não uma assinatura qualificada baseada em certificado ICP-Brasil.
