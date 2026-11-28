# Estrutura do projeto

Arquivos `.env` e diretórios gerados (`.git`, `node_modules`, `target`, `dist`) foram omitidos.

```text
odontologia-api
|-- .agents
|   \-- skills
|       |-- openspec-apply-change
|       |   \-- SKILL.md
|       |-- openspec-archive-change
|       |   \-- SKILL.md
|       |-- openspec-explore
|       |   \-- SKILL.md
|       |-- openspec-propose
|       |   \-- SKILL.md
|       |-- openspec-sync-specs
|       |   \-- SKILL.md
|       |-- openspec-update-change
|       |   \-- SKILL.md
|       \-- .openspec-target
|-- .claude
|   |-- commands
|   |   \-- opsx
|   |       |-- apply.md
|   |       |-- archive.md
|   |       |-- explore.md
|   |       |-- propose.md
|   |       |-- sync.md
|   |       \-- update.md
|   \-- skills
|       |-- openspec-apply-change
|       |   \-- SKILL.md
|       |-- openspec-archive-change
|       |   \-- SKILL.md
|       |-- openspec-explore
|       |   \-- SKILL.md
|       |-- openspec-propose
|       |   \-- SKILL.md
|       |-- openspec-sync-specs
|       |   \-- SKILL.md
|       \-- openspec-update-change
|           \-- SKILL.md
|-- .github
|   \-- modernize
|       \-- java-upgrade
|           |-- hooks
|           |   \-- scripts
|           |       |-- recordToolUse.ps1
|           |       \-- recordToolUse.sh
|           \-- .gitignore
|-- .opencode
|   |-- commands
|   |   |-- opsx-apply.md
|   |   |-- opsx-archive.md
|   |   |-- opsx-explore.md
|   |   |-- opsx-propose.md
|   |   |-- opsx-sync.md
|   |   \-- opsx-update.md
|   \-- skills
|       |-- openspec-apply-change
|       |   \-- SKILL.md
|       |-- openspec-archive-change
|       |   \-- SKILL.md
|       |-- openspec-explore
|       |   \-- SKILL.md
|       |-- openspec-propose
|       |   \-- SKILL.md
|       |-- openspec-sync-specs
|       |   \-- SKILL.md
|       \-- openspec-update-change
|           \-- SKILL.md
|-- Arquivos de ES
|   |-- Docs
|   |   |-- Arquitetura_do_Sistema_Odonto_Uni_v3.pdf
|   |   |-- Diagrama_de_Classes_projES.docx
|   |   |-- Diagramas_de_Atividade_projES.docx
|   |   \-- Evidências de validação.pdf
|   |-- Documento de Requisitos e Histórias de Usuário – Sistema de Gerenciamento de Procedimentos Odontológicos.pdf
|   \-- Link de acesso Figma - Odonto Uni.txt
|-- backend
|   |-- .mvn
|   |   \-- wrapper
|   |       \-- maven-wrapper.properties
|   |-- src
|   |   |-- main
|   |   |   |-- java
|   |   |   |   \-- br
|   |   |   |       \-- com
|   |   |   |           \-- unicatolica
|   |   |   |               \-- odontologia_api
|   |   |   |                   |-- config
|   |   |   |                   |   \-- SecurityConfig.java
|   |   |   |                   |-- consultas
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   |-- AgendamentoController.java
|   |   |   |                   |   |   |-- CadeiraController.java
|   |   |   |                   |   |   |-- FaltaController.java
|   |   |   |                   |   |   \-- VagaController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- AgendamentoRequestDTO.java
|   |   |   |                   |   |   |-- AgendamentoResponseDTO.java
|   |   |   |                   |   |   |-- AlunoAgendamentoDTO.java
|   |   |   |                   |   |   |-- AusenciaResponseDTO.java
|   |   |   |                   |   |   |-- BloqueioTurnoRequestDTO.java
|   |   |   |                   |   |   |-- BloqueioVagaRequestDTO.java
|   |   |   |                   |   |   |-- CadeiraResponseDTO.java
|   |   |   |                   |   |   |-- DisponibilidadeDTO.java
|   |   |   |                   |   |   |-- FaltaRequestDTO.java
|   |   |   |                   |   |   |-- FilaAgendamentoDTO.java
|   |   |   |                   |   |   |-- GerarTurnosRequestDTO.java
|   |   |   |                   |   |   |-- GerarTurnosResponseDTO.java
|   |   |   |                   |   |   |-- NotificacaoConsultaDTO.java
|   |   |   |                   |   |   |-- ReagendamentoRequestDTO.java
|   |   |   |                   |   |   |-- RemarcacaoRequestDTO.java
|   |   |   |                   |   |   |-- TurnoGradeDTO.java
|   |   |   |                   |   |   |-- VagaRequestDTO.java
|   |   |   |                   |   |   \-- VagaResponseDTO.java
|   |   |   |                   |   |-- entity
|   |   |   |                   |   |   |-- Consulta.java
|   |   |   |                   |   |   |-- NotificacaoWhatsapp.java
|   |   |   |                   |   |   |-- StatusConsulta.java
|   |   |   |                   |   |   \-- Vaga.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   |-- AgendaConsultaRepository.java
|   |   |   |                   |   |   |-- ConsultaRepository.java
|   |   |   |                   |   |   |-- NotificacaoWhatsappRepository.java
|   |   |   |                   |   |   |-- VagaGestaoRepository.java
|   |   |   |                   |   |   \-- VagaRepository.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       |-- AgendamentoService.java
|   |   |   |                   |       |-- ConsultaFaltaService.java
|   |   |   |                   |       |-- GestaoVagasService.java
|   |   |   |                   |       \-- NotificacaoAgendamentoService.java
|   |   |   |                   |-- controller
|   |   |   |                   |   |-- AutenticacaoController.java
|   |   |   |                   |   |-- CatalogoSolicitacaoController.java
|   |   |   |                   |   |-- DesempenhoAcademicoController.java
|   |   |   |                   |   |-- DesempenhoAcademicoExceptionHandler.java
|   |   |   |                   |   |-- MetaAlunoController.java
|   |   |   |                   |   |-- MetricasCoordenadorController.java
|   |   |   |                   |   |-- MetricasCoordenadorExceptionHandler.java
|   |   |   |                   |   \-- PacienteController.java
|   |   |   |                   |-- dto
|   |   |   |                   |   |-- DesempenhoAcademicoResponse.java
|   |   |   |                   |   |-- DesempenhoErroResponse.java
|   |   |   |                   |   |-- EsqueciSenhaRequest.java
|   |   |   |                   |   |-- LoginDTO.java
|   |   |   |                   |   |-- LoginResponseDTO.java
|   |   |   |                   |   |-- MensagemAuthResponse.java
|   |   |   |                   |   |-- MetaProcedimentoResponse.java
|   |   |   |                   |   |-- MetricasCoordenadorResponse.java
|   |   |   |                   |   |-- MetricasErroResponse.java
|   |   |   |                   |   |-- PainelMetasResponse.java
|   |   |   |                   |   |-- ProgressoDisciplinaResponse.java
|   |   |   |                   |   |-- RedefinirSenhaRequest.java
|   |   |   |                   |   \-- SemestresLetivosResponse.java
|   |   |   |                   |-- entity
|   |   |   |                   |   |-- ProgressoMetaAluno.java
|   |   |   |                   |   \-- ProgressoMetaAlunoId.java
|   |   |   |                   |-- exception
|   |   |   |                   |   |-- DesempenhoAcademicoException.java
|   |   |   |                   |   |-- SemestreInvalidoException.java
|   |   |   |                   |   \-- SemestreNaoEncontradoException.java
|   |   |   |                   |-- filaespera
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   \-- FilaEsperaController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- EntradaFilaCriadaDTO.java
|   |   |   |                   |   |   |-- FilaEsperaAtualizadaDTO.java
|   |   |   |                   |   |   |-- FilaEsperaItemDTO.java
|   |   |   |                   |   |   |-- NovaEntradaFilaDTO.java
|   |   |   |                   |   |   \-- TempoMedioEsperaDTO.java
|   |   |   |                   |   |-- entity
|   |   |   |                   |   |   \-- FilaEspera.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   \-- FilaEsperaRepository.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       \-- FilaEsperaService.java
|   |   |   |                   |-- model
|   |   |   |                   |   |-- Paciente.java
|   |   |   |                   |   |-- PasswordResetToken.java
|   |   |   |                   |   |-- Perfil.java
|   |   |   |                   |   \-- Usuario.java
|   |   |   |                   |-- notificacoes
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   \-- NotificacaoController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- NotificacaoResumoDTO.java
|   |   |   |                   |   |   \-- ProcessamentoLembretesDTO.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   |-- CandidatoLembrete24h.java
|   |   |   |                   |   |   |-- Lembrete24hRepository.java
|   |   |   |                   |   |   \-- NotificacaoConsultaQueryRepository.java
|   |   |   |                   |   |-- service
|   |   |   |                   |   |   |-- Lembrete24hService.java
|   |   |   |                   |   |   |-- LinkWhatsapp.java
|   |   |   |                   |   |   |-- NotificacaoConclusaoService.java
|   |   |   |                   |   |   \-- NotificacaoErrorLogger.java
|   |   |   |                   |   \-- webhook
|   |   |   |                   |       |-- NotificacaoPendenteWebhook.java
|   |   |   |                   |       |-- ReceberWebhookDTO.java
|   |   |   |                   |       |-- RespostaWebhookDTO.java
|   |   |   |                   |       |-- WebhookWhatsappController.java
|   |   |   |                   |       \-- WebhookWhatsappService.java
|   |   |   |                   |-- prontuarios
|   |   |   |                   |   |-- config
|   |   |   |                   |   |   \-- ProntuarioClockConfig.java
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   |-- ProntuarioController.java
|   |   |   |                   |   |   \-- ProntuarioExceptionHandler.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- ConsultaClinica.java
|   |   |   |                   |   |   |-- ObservacaoOdontogramaRequest.java
|   |   |   |                   |   |   |-- ProntuarioHistoricoResponse.java
|   |   |   |                   |   |   |-- RegistrarAdendoRequest.java
|   |   |   |                   |   |   |-- RegistrarProntuarioRequest.java
|   |   |   |                   |   |   |-- SalvarAdendoResponse.java
|   |   |   |                   |   |   \-- SalvarProntuarioResponse.java
|   |   |   |                   |   |-- entity
|   |   |   |                   |   |   |-- PacienteAnamnese.java
|   |   |   |                   |   |   |-- PacienteOdontograma.java
|   |   |   |                   |   |   |-- Prontuario.java
|   |   |   |                   |   |   \-- ProntuarioAdendo.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   |-- ProntuarioAdendoJpaRepository.java
|   |   |   |                   |   |   |-- ProntuarioJpaRepository.java
|   |   |   |                   |   |   \-- ProntuarioQueryRepository.java
|   |   |   |                   |   |-- security
|   |   |   |                   |   |   |-- CidrMatcher.java
|   |   |   |                   |   |   |-- ProntuarioNetworkFilter.java
|   |   |   |                   |   |   \-- ProntuarioProperties.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       |-- AutorizacaoProntuarioService.java
|   |   |   |                   |       \-- ProntuarioService.java
|   |   |   |                   |-- repository
|   |   |   |                   |   |-- DesempenhoAcademicoRepository.java
|   |   |   |                   |   |-- MetricasCoordenadorRepository.java
|   |   |   |                   |   |-- PacienteRepository.java
|   |   |   |                   |   |-- PasswordResetTokenRepository.java
|   |   |   |                   |   |-- ProgressoMetaAlunoRepository.java
|   |   |   |                   |   \-- UsuarioRepository.java
|   |   |   |                   |-- retornos
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   \-- RetornoController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- AtendimentoDoTurnoDTO.java
|   |   |   |                   |   |   |-- RetornoResponse.java
|   |   |   |                   |   |   \-- SugestaoRetornoRequest.java
|   |   |   |                   |   |-- entity
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   \-- ConsultaRetornoRepository.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       \-- RetornoService.java
|   |   |   |                   |-- satisfacao
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   \-- SatisfacaoCoordenadorController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   \-- RelatorioSatisfacaoResponse.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   \-- RelatorioSatisfacaoRepository.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       \-- RelatorioSatisfacaoService.java
|   |   |   |                   |-- security
|   |   |   |                   |   |-- PasswordRecoveryRateLimiter.java
|   |   |   |                   |   |-- PasswordResetCodeSender.java
|   |   |   |                   |   |-- PasswordResetDeliveryService.java
|   |   |   |                   |   \-- SessaoAluno.java
|   |   |   |                   |-- service
|   |   |   |                   |   |-- AutenticacaoService.java
|   |   |   |                   |   |-- DesempenhoAcademicoService.java
|   |   |   |                   |   |-- MetaAlunoService.java
|   |   |   |                   |   |-- MetricasCoordenadorService.java
|   |   |   |                   |   |-- PacienteService.java
|   |   |   |                   |   \-- PasswordRecoveryService.java
|   |   |   |                   |-- solicitacoes
|   |   |   |                   |   |-- controller
|   |   |   |                   |   |   |-- SolicitacaoConflitoHandler.java
|   |   |   |                   |   |   \-- SolicitacaoPacienteController.java
|   |   |   |                   |   |-- dto
|   |   |   |                   |   |   |-- AprovarSolicitacaoDTO.java
|   |   |   |                   |   |   |-- NovaSolicitacaoDTO.java
|   |   |   |                   |   |   |-- RecusarSolicitacaoDTO.java
|   |   |   |                   |   |   |-- SolicitacaoAdministrativaDTO.java
|   |   |   |                   |   |   \-- SolicitacaoAdministrativaProjection.java
|   |   |   |                   |   |-- entity
|   |   |   |                   |   |   \-- SolicitacaoPaciente.java
|   |   |   |                   |   |-- repository
|   |   |   |                   |   |   |-- SolicitacaoPacienteRepository.java
|   |   |   |                   |   |   \-- UsuarioIdentidadeRepository.java
|   |   |   |                   |   \-- service
|   |   |   |                   |       \-- SolicitacaoPacienteService.java
|   |   |   |                   \-- OdontologiaApiApplication.java
|   |   |   \-- resources
|   |   |       |-- db
|   |   |       |   \-- migration
|   |   |       |       |-- V1__init_enums_and_extensions.sql
|   |   |       |       |-- V10__insert_dados_iniciais.sql
|   |   |       |       |-- V11__constraints_complementares.sql
|   |   |       |       |-- V12__separar_observacao_secretaria.sql
|   |   |       |       |-- V13__adicionar_uf_pacientes.sql
|   |   |       |       |-- V14__permitir_justificativa_para_falta_de_aluno.sql
|   |   |       |       |-- V15__garantir_templates_agendamento.sql
|   |   |       |       |-- V16__create_password_reset_tokens.sql
|   |   |       |       |-- V17__unique_lembrete_whatsapp_por_consulta.sql
|   |   |       |       |-- V18__unique_satisfaction_notification_per_consultation.sql
|   |   |       |       |-- V2__create_usuarios_and_pacientes.sql
|   |   |       |       |-- V3__create_triagens_and_metas.sql
|   |   |       |       |-- V4__create_fila_and_solicitacoes.sql
|   |   |       |       |-- V5__create_consultas_and_transferencias.sql
|   |   |       |       |-- V6__create_prontuarios.sql
|   |   |       |       |-- V7__create_whatsapp_and_satisfaction.sql
|   |   |       |       |-- V8__create_triggers_and_views.sql
|   |   |       |       \-- V9__create_prontuario_fisico_detalhes.sql
|   |   |       |-- META-INF
|   |   |       |   \-- additional-spring-configuration-metadata.json
|   |   |       |-- application.properties
|   |   |       \-- application-local.properties
|   |   \-- test
|   |       |-- java
|   |       |   \-- br
|   |       |       \-- com
|   |       |           \-- unicatolica
|   |       |               \-- odontologia_api
|   |       |                   |-- consultas
|   |       |                   |   |-- controller
|   |       |                   |   |   |-- CadeiraControllerTest.java
|   |       |                   |   |   \-- VagaControllerTest.java
|   |       |                   |   |-- repository
|   |       |                   |   |   |-- VagaGestaoRepositorySqlTest.java
|   |       |                   |   |   |-- VagaRepositoryAvailabilityTest.java
|   |       |                   |   |   \-- VagaUniqueConstraintTest.java
|   |       |                   |   \-- service
|   |       |                   |       |-- AgendamentoServiceTest.java
|   |       |                   |       |-- ConsultaFaltaServiceTest.java
|   |       |                   |       |-- GestaoVagasServiceTest.java
|   |       |                   |       \-- NotificacaoAgendamentoServiceTest.java
|   |       |                   |-- controller
|   |       |                   |   |-- DesempenhoAcademicoControllerTest.java
|   |       |                   |   \-- MetricasCoordenadorControllerTest.java
|   |       |                   |-- filaespera
|   |       |                   |   |-- controller
|   |       |                   |   |   \-- FilaEsperaControllerTest.java
|   |       |                   |   \-- service
|   |       |                   |       \-- FilaEsperaServiceTest.java
|   |       |                   |-- notificacoes
|   |       |                   |   |-- controller
|   |       |                   |   |   \-- NotificacaoControllerTest.java
|   |       |                   |   |-- repository
|   |       |                   |   |   \-- NotificacaoConsultaQueryRepositoryTest.java
|   |       |                   |   |-- service
|   |       |                   |   |   |-- Lembrete24hServiceTest.java
|   |       |                   |   |   \-- LinkWhatsappTest.java
|   |       |                   |   \-- webhook
|   |       |                   |       |-- WebhookWhatsappControllerTest.java
|   |       |                   |       |-- WebhookWhatsappPostgresIntegrationTest.java
|   |       |                   |       \-- WebhookWhatsappServiceTest.java
|   |       |                   |-- prontuarios
|   |       |                   |   |-- security
|   |       |                   |   |   |-- CidrMatcherTest.java
|   |       |                   |   |   \-- ProntuarioNetworkFilterTest.java
|   |       |                   |   |-- service
|   |       |                   |   |   \-- AutorizacaoProntuarioServiceTest.java
|   |       |                   |   \-- ProntuarioSupabaseIntegrationTest.java
|   |       |                   |-- repository
|   |       |                   |   \-- DesempenhoAcademicoRepositoryTest.java
|   |       |                   |-- retornos
|   |       |                   |   \-- service
|   |       |                   |       \-- RetornoServiceTest.java
|   |       |                   |-- satisfacao
|   |       |                   |   |-- controller
|   |       |                   |   |   \-- SatisfacaoCoordenadorControllerTest.java
|   |       |                   |   |-- repository
|   |       |                   |   |   \-- RelatorioSatisfacaoRepositoryH2Test.java
|   |       |                   |   \-- service
|   |       |                   |       \-- RelatorioSatisfacaoServiceTest.java
|   |       |                   |-- service
|   |       |                   |   |-- AutenticacaoServiceTest.java
|   |       |                   |   |-- DesempenhoAcademicoServiceTest.java
|   |       |                   |   |-- MetaAlunoServiceTest.java
|   |       |                   |   |-- MetricasCoordenadorServiceTest.java
|   |       |                   |   \-- PacienteFichaFisicaTest.java
|   |       |                   |-- solicitacoes
|   |       |                   |   |-- controller
|   |       |                   |   |   |-- RecusaSolicitacaoControllerTest.java
|   |       |                   |   |   |-- SolicitacaoPacienteControllerTest.java
|   |       |                   |   |   \-- SolicitacaoPacientePermissoesTest.java
|   |       |                   |   \-- service
|   |       |                   |       |-- RecusaSolicitacaoServiceTest.java
|   |       |                   |       \-- SolicitacaoPacienteServiceTest.java
|   |       |                   |-- AutenticacaoSegurancaMvcTest.java
|   |       |                   |-- CsrfTestSupport.java
|   |       |                   |-- OdontologiaApiApplicationTests.java
|   |       |                   |-- PasswordRecoveryIntegrationTest.java
|   |       |                   |-- SecurityAuditIntegrationTest.java
|   |       |                   \-- SupabaseSchemaAuditIntegrationTest.java
|   |       \-- resources
|   |           |-- sql
|   |           |   |-- seed-gestao-faltas-local.sql
|   |           |   \-- vaga-constraint-schema.sql
|   |           |-- application-supabase-test.properties
|   |           \-- application-test.properties
|   |-- mvnw
|   |-- mvnw.cmd
|   \-- pom.xml
|-- docs
|   |-- css_faltas_agend
|   |   \-- css.txt
|   |-- rf010-validacao-solicitacoes
|   |   |-- figma
|   |   |   |-- analisar-vincular.png.png
|   |   |   \-- solicitacoes-alunos.png.png
|   |   |-- plan.md
|   |   |-- spec.md
|   |   |-- tasks.md
|   |   \-- validacao-isolada.md
|   |-- sdd
|   |   |-- design-faltas.md
|   |   |-- requisitos-faltas.md
|   |   |-- requisitos-frontend-faltas.md
|   |   |-- spec-faltas.md
|   |   |-- tasks-faltas.md
|   |   |-- tasks-frontend-faltas.md
|   |   \-- user-stories-faltas.md
|   |-- sql_banco
|   |   \-- schema_completo.sql
|   |-- api-agenda-oficial.md
|   |-- api-gestao-vagas.md
|   |-- api-turnos-disponiveis.md
|   |-- autenticacao-e-recuperacao.md
|   |-- relatorio-testes-seguranca.md
|   \-- teste-local-faltas.md
|-- frontend
|   |-- public
|   |   |-- favicon.svg
|   |   \-- icons.svg
|   |-- src
|   |   |-- Aluno
|   |   |   \-- AplicacaoAluno.jsx
|   |   |-- assets
|   |   |   |-- clinic-login.png
|   |   |   |-- hero.png
|   |   |   |-- react.svg
|   |   |   \-- vite.svg
|   |   |-- Coordenador
|   |   |   |-- DesempenhoHistoricoAcademico.css
|   |   |   |-- DesempenhoHistoricoAcademico.jsx
|   |   |   |-- PainelCoordenador.css
|   |   |   |-- PainelCoordenador.jsx
|   |   |   |-- RelatorioSatisfacao.css
|   |   |   \-- RelatorioSatisfacao.jsx
|   |   |-- data
|   |   |   \-- pacientes.js
|   |   |-- Login
|   |   |   |-- Login.css
|   |   |   \-- Login.jsx
|   |   |-- Pacientes
|   |   |   |-- CadastroPaciente.css
|   |   |   \-- CadastroPaciente.jsx
|   |   |-- PainelAluno
|   |   |   |-- PacientesDoTurno.jsx
|   |   |   |-- PainelAluno.css
|   |   |   |-- PainelAluno.jsx
|   |   |   |-- ProntuarioDigital.css
|   |   |   \-- ProntuarioDigital.jsx
|   |   |-- Retornos
|   |   |   |-- EncerrarAtendimento.css
|   |   |   \-- EncerrarAtendimento.jsx
|   |   |-- Secretaria
|   |   |   |-- AgendamentosSecretaria.css
|   |   |   |-- AgendamentosSecretaria.jsx
|   |   |   |-- AgendaSecretaria.css
|   |   |   |-- AgendaSecretaria.jsx
|   |   |   |-- ComunicacaoWhatsapp.css
|   |   |   |-- ComunicacaoWhatsapp.jsx
|   |   |   |-- PainelReagendamento.jsx
|   |   |   |-- PainelRegistroFalta.jsx
|   |   |   |-- TabelaHistoricoAusencias.jsx
|   |   |   \-- TurnosDisponiveisSecretaria.jsx
|   |   |-- service
|   |   |   \-- FilaEsperaService.js
|   |   |-- services
|   |   |   |-- agendamentoService.js
|   |   |   |-- autenticacaoService.js
|   |   |   |-- desempenhoAcademicoService.js
|   |   |   |-- FaltasService.js
|   |   |   |-- metaService.js
|   |   |   |-- metricasCoordenadorService.js
|   |   |   |-- prontuarioService.js
|   |   |   |-- retornoService.js
|   |   |   |-- satisfacaoCoordenadorService.js
|   |   |   |-- solicitacaoService.js
|   |   |   \-- VagasService.js
|   |   |-- AnalisarSolicitacao.css
|   |   |-- AnalisarSolicitacao.jsx
|   |   |-- api.js
|   |   |-- App.css
|   |   |-- App.jsx
|   |   |-- FilaEspera.css
|   |   |-- FilaEspera.jsx
|   |   |-- index.css
|   |   |-- main.jsx
|   |   |-- style.css
|   |   |-- ValidacaoSolicitacoes.css
|   |   \-- ValidacaoSolicitacoes.jsx
|   |-- tests
|   |   |-- rf004.test.mjs
|   |   |-- rf009.test.mjs
|   |   |-- rf010.test.mjs
|   |   |-- rf010-fixtures.mjs
|   |   |-- rf013.test.mjs
|   |   |-- rf016.test.mjs
|   |   \-- servidor-isolado.mjs
|   |-- .gitignore
|   |-- eslint.config.js
|   |-- index.html
|   |-- package.json
|   |-- package-lock.json
|   |-- README.md
|   \-- vite.config.js
|-- openspec
|   |-- changes
|   |   |-- ajustes-tecnicos-arquitetura
|   |   |   |-- specs
|   |   |   |   |-- autorizacao-perfis-api
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- cadastro-paciente-ficha-fisica
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- consultas-mapeamento-unificado
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- integridade-migrations-flyway
|   |   |   |   |   \-- spec.md
|   |   |   |   \-- prontuario-horarios-configuraveis
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- archive
|   |   |   |-- 2026-11-20-rf004-registro-informacoes-prontuario-aluno
|   |   |   |   |-- specs
|   |   |   |   |   |-- immutable-clinical-records
|   |   |   |   |   |   \-- spec.md
|   |   |   |   |   |-- patient-clinical-history
|   |   |   |   |   |   \-- spec.md
|   |   |   |   |   |-- student-clinical-access
|   |   |   |   |   |   \-- spec.md
|   |   |   |   |   \-- student-clinical-record-ui
|   |   |   |   |       \-- spec.md
|   |   |   |   |-- .openspec.yaml
|   |   |   |   |-- design.md
|   |   |   |   |-- proposal.md
|   |   |   |   \-- tasks.md
|   |   |   \-- .gitkeep
|   |   |-- cadastro-gestao-vagas
|   |   |   |-- specs
|   |   |   |   \-- gestao-vagas
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- disponibilizacao-turnos-disponiveis
|   |   |   |-- specs
|   |   |   |   \-- turnos-disponiveis
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- gestao-consultas-agenda-oficial
|   |   |   |-- specs
|   |   |   |   \-- agenda-oficial
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- integracao-whatsapp-rf009
|   |   |   |-- assets
|   |   |   |   |-- evidence
|   |   |   |   |   |-- rf009-desktop-1440x900.png
|   |   |   |   |   |-- rf009-mobile-390x844.png
|   |   |   |   |   \-- rf009-mobile-lower-390x844.png
|   |   |   |   \-- comunicacao-whatsapp.png
|   |   |   |-- specs
|   |   |   |   |-- secretaria-whatsapp-communication
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- whatsapp-notifications
|   |   |   |   |   \-- spec.md
|   |   |   |   \-- whatsapp-replies
|   |   |   |       \-- spec.md
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- reorganizar-backend-sanar-lint
|   |   |   |-- specs
|   |   |   |   |-- autorizacao-perfis-api
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- backend-layout
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- cadastro-paciente-ficha-fisica
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- consultas-mapeamento-unificado
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- frontend-lint-quality
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- integridade-migrations-flyway
|   |   |   |   |   \-- spec.md
|   |   |   |   \-- prontuario-horarios-configuraveis
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- rf004-prontuario-pagina-contextual
|   |   |   |-- specs
|   |   |   |   |-- immutable-clinical-records
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- patient-clinical-history
|   |   |   |   |   \-- spec.md
|   |   |   |   |-- student-clinical-access
|   |   |   |   |   \-- spec.md
|   |   |   |   \-- student-clinical-record-page
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   \-- tasks.md
|   |   |-- rf013-desempenho-historico-academico
|   |   |   |-- specs
|   |   |   |   \-- desempenho-historico-academico
|   |   |   |       \-- spec.md
|   |   |   |-- .openspec.yaml
|   |   |   |-- design.md
|   |   |   |-- proposal.md
|   |   |   |-- ROTEIRO_SPEC_DRIVEN.md
|   |   |   \-- tasks.md
|   |   \-- rf016-login-password-recovery
|   |       |-- specs
|   |       |   |-- institutional-login
|   |       |   |   \-- spec.md
|   |       |   \-- password-recovery
|   |       |       \-- spec.md
|   |       |-- .openspec.yaml
|   |       |-- design.md
|   |       |-- proposal.md
|   |       \-- tasks.md
|   |-- specs
|   |   \-- .gitkeep
|   \-- config.yaml
|-- .gitattributes
|-- .gitignore
\-- README.md
```
