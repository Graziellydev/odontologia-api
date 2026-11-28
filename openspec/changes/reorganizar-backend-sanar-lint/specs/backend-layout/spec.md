# Spec Delta

## Purpose

Define como o backend deve ser localizado, configurado e executado após seu isolamento em uma pasta própria no repositório.

## ADDED Requirements

### Requirement: Backend executável na pasta dedicada
O projeto Spring Boot SHALL ser compilável e executável a partir de `backend/`, mantendo frontend e OpenSpec na raiz.

#### Scenario: Build Maven no backend
- **WHEN** a pessoa desenvolvedora executa Maven dentro de `backend/`
- **THEN** o projeto compila usando wrapper e recursos relativos ao backend

### Requirement: Configuração compartilhada na raiz
O backend SHALL resolver arquivos de configuração compartilhados na raiz por caminhos relativos à nova localização.

#### Scenario: Import de configuração raiz
- **WHEN** Spring Boot é iniciado no diretório `backend/` e a configuração importa `.env` opcionalmente
- **THEN** a importação aponta para `../.env` sem alterar esse arquivo
