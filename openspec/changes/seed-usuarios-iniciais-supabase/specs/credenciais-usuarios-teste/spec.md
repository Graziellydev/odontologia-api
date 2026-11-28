# Spec Delta

## Purpose

Permite que a equipe consulte e use de forma consistente as contas iniciais de teste, identificando os perfis disponíveis e as credenciais necessárias para autenticação manual.

## ADDED Requirements

### Requirement: Documentação das contas de teste
O repositório SHALL conter `usuarios_sistema.md` na raiz com uma tabela dos 13 usuários provisionados, incluindo perfil, nome, e-mail, matrícula e senha de teste, além de instruções breves para entrar no sistema.

#### Scenario: Consulta das credenciais
- **WHEN** uma pessoa desenvolvedora abre `usuarios_sistema.md`
- **THEN** encontra as 2 contas SECRETARIA, 10 ALUNO e 1 COORDENADOR e seus dados de acesso correspondentes à migration

#### Scenario: Orientação de uso
- **WHEN** uma pessoa usa uma conta documentada para validar o sistema
- **THEN** o documento informa como acessar a aplicação e esclarece que as credenciais são exclusivamente de teste e devem ser rotacionadas antes de qualquer uso operacional
