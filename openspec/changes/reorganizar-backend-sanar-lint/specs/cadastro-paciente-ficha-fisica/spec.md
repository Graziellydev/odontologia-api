# Spec Delta

## Purpose

Garante ida e volta entre API e banco para UF e os dados completos da ficha física de pacientes.

## ADDED Requirements

### Requirement: Dados físicos persistidos e retornados
O sistema SHALL persistir e retornar UF, RG, órgão expedidor, sexo, raça/cor, local de moradia, tipo de escola, escolaridades do paciente e responsáveis, e renda familiar.

#### Scenario: Criar e consultar ficha completa
- **WHEN** a secretaria cria ou atualiza paciente com os campos físicos
- **THEN** a leitura posterior retorna os mesmos valores sem perda

### Requirement: Colunas CHAR compatíveis
O sistema SHALL validar os tipos fixos de UF e sexo contra PostgreSQL em inicialização com validação ORM.

#### Scenario: Validação Hibernate
- **WHEN** a aplicação verifica o schema PostgreSQL
- **THEN** UF e sexo são reconhecidos como CHAR compatível
