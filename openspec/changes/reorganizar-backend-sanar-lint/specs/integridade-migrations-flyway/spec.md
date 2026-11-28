# Spec Delta

## Purpose

Mantém as migrações aplicadas verificáveis por checksum e limita alterações estruturais a novas versões sequenciais.

## ADDED Requirements

### Requirement: Histórico Flyway íntegro
O sistema SHALL validar checksums das migrations aplicadas V2–V9 contra os bytes históricos correspondentes ao ambiente Supabase.

#### Scenario: Flyway valida histórico
- **WHEN** a aplicação conecta a um banco já migrado
- **THEN** Flyway conclui validação sem divergência de checksum

### Requirement: Novas alterações em migration sequencial
Qualquer alteração estrutural necessária SHALL ser implementada somente em V19 idempotente, sem baseline automático.

#### Scenario: Objeto estrutural já existe
- **WHEN** a migration V19 encontra o objeto alvo existente
- **THEN** sua aplicação não falha por duplicação
