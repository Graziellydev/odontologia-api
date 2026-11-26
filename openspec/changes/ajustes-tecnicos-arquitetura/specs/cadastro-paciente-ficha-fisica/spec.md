# Spec Delta

## Purpose

Garante que os dados de identificação e socioeconômicos já previstos no schema sejam mantidos integralmente no cadastro do paciente.

## ADDED Requirements

### Requirement: Cadastro completo da ficha física
O sistema SHALL aceitar e persistir UF, RG, órgão expedidor, sexo, raça/cor, local de moradia, tipo de escola, escolaridades do paciente, mãe e pai, e renda familiar no cadastro de paciente.

#### Scenario: Cadastro com ficha física completa
- **WHEN** a secretaria cadastra um paciente com todos os campos da ficha física válidos
- **THEN** a resposta criada contém os valores persistidos

### Requirement: Edição completa da ficha física
O sistema SHALL permitir atualizar todos os campos da ficha física e UF sem descartar os demais dados cadastrais editáveis.

#### Scenario: Atualização de UF e escolaridade
- **WHEN** a secretaria atualiza a UF e uma escolaridade de um paciente existente
- **THEN** a consulta posterior retorna os novos valores

### Requirement: Detalhamento fiel do paciente
O sistema SHALL retornar UF e todos os campos da ficha física nas representações de detalhe e listagem que expõem dados cadastrais do paciente.

#### Scenario: Consulta por identificador
- **WHEN** a secretaria consulta um paciente que possui dados de ficha física
- **THEN** a resposta inclui cada campo cadastrado sem conversão ou truncamento indevido

### Requirement: Compatibilidade de tipos de código fixo
O sistema SHALL validar o mapeamento ORM de UF e sexo contra colunas PostgreSQL de largura fixa sem falhar na validação de schema.

#### Scenario: Inicialização com validação de schema
- **WHEN** a aplicação inicia com validação de schema habilitada contra PostgreSQL
- **THEN** o mapeamento de UF e sexo é aceito como compatível com as colunas CHAR existentes
