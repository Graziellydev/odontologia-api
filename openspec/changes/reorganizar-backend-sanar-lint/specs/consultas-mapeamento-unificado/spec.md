# Spec Delta

## Purpose

Evita que os fluxos de agendamento e retorno sobrescrevam silenciosamente atualizações concorrentes da mesma consulta.

## ADDED Requirements

### Requirement: Consulta tem versão canônica
O sistema SHALL atualizar cada linha de consultas por uma representação JPA única com controle otimista de versão.

#### Scenario: Duas atualizações concorrentes
- **WHEN** agendamento e retorno atualizam a mesma versão da consulta
- **THEN** a gravação obsoleta falha como conflito sem sobrescrever dados
