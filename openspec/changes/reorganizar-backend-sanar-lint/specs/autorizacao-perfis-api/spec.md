# Spec Delta

## Purpose

Padroniza autoridades e garante proteção uniforme das rotas de retornos, metas, catálogo e fila.

## ADDED Requirements

### Requirement: Authorities canônicas
As regras de autorização SHALL utilizar exclusivamente SECRETARIA, ALUNO e COORDENADOR sem aliases ROLE_*.

#### Scenario: Perfil divergente
- **WHEN** usuário autenticado com perfil não autorizado chama uma rota protegida
- **THEN** a API responde HTTP 403

### Requirement: Endpoints protegidos em duas camadas
Retornos, metas e catálogo SHALL exigir ALUNO; fila de espera SHALL exigir SECRETARIA na configuração HTTP e no controlador.

#### Scenario: Acesso cruzado
- **WHEN** perfil diferente do exigido acessa qualquer uma das quatro famílias de rota
- **THEN** a API responde HTTP 403
