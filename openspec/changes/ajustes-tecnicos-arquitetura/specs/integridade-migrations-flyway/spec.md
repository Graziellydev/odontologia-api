# Spec Delta

## Purpose

Preserva a integridade do histórico de migrations já aplicado e torna qualquer evolução estrutural futura rastreável e repetível.

## ADDED Requirements

### Requirement: Checksum histórico preservado
O sistema SHALL manter as migrations V2 a V9 byte a byte compatíveis com a revisão Git que corresponde ao histórico já aplicado no ambiente Supabase.

#### Scenario: Validação de migrations existentes
- **WHEN** o Flyway valida um banco que já executou V2 a V9
- **THEN** a validação não reporta divergência de checksum para essas versões

### Requirement: Baseline implícito desabilitado
O sistema SHALL executar Flyway sem habilitar baseline automático em migração.

#### Scenario: Banco sem histórico Flyway
- **WHEN** o Flyway é iniciado contra um banco não vazio sem histórico
- **THEN** ele não cria baseline automaticamente e informa a condição de migração necessária

### Requirement: Evolução estrutural sequencial
O sistema SHALL registrar qualquer correção estrutural adicional em uma migration V19 nova, idempotente e sem reescrever migrations previamente aplicadas.

#### Scenario: Reexecução segura de V19
- **WHEN** a migration V19 é avaliada em um schema que já contém o objeto alvo
- **THEN** ela não falha por tentar recriar coluna, índice ou constraint existente
