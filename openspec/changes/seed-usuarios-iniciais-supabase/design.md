# Design

## Context

Ver `proposal.md` e as capacidades em `specs/`. V1 define `usuarios.perfil` como texto validado por CHECK; V2 exige `nome`, `email`, `senha_hash` e `perfil`, e impõe unicidade em `email` e `matricula_ou_registro`. Os demais campos necessários têm defaults ou aceitam nulo. V10 insere procedimentos e templates, não usuários. A aplicação fornece `BCryptPasswordEncoder` sem custo customizado, portanto usa o custo padrão do encoder.

## Goals / Non-Goals

**Goals:** adicionar V19 sequencial, permitir execução repetida sem sobrescrita, validar persistência e autenticação BCrypt no Supabase e documentar o conjunto de teste.

**Non-Goals:** alterar o schema, mudar o encoder ou provisionar contas de produção.

## Decisions

- V19 insere somente `nome`, `email`, `senha_hash`, `perfil` e `matricula_ou_registro`; deixa semestre nulo e usa os defaults existentes para `ativo` e timestamps.
- Os hashes BCrypt são gerados com o `BCryptPasswordEncoder` existente e persistidos como constantes SQL. As senhas em texto aparecem somente no documento de contas de teste e nos dados de teste da verificação, nunca na migration.
- `ON CONFLICT DO NOTHING` cobre as constraints únicas de e-mail e matrícula sem alterar registros preexistentes. A verificação compara as 13 combinações e exige os dados esperados; se uma identidade colidir com uma conta divergente, a aplicação não sobrescreve a conta e a validação falha com a divergência identificada.
- A aplicação Supabase ocorre em execução única usando Flyway e credenciais exportadas apenas para o processo, lidas da configuração local autorizada. A saída não deve imprimir URL, usuário, senha, hash, nem conteúdo do `.env`. Não executar `clean` nem alterar `.env`.
- A validação automatizada consulta os 13 registros por e-mail/matrícula, verifica as três contagens de perfil e usa `PasswordEncoder.matches` para cada senha. A mesma migration é então reexecutada/validada para comprovar idempotência.
- `usuarios_sistema.md` contém as senhas solicitadas porque é documentação explícita de contas de teste; identifica o arquivo como não operacional e exige rotação antes de uso real.

## Risks / Trade-offs

- [E-mail ou matrícula já associado a outra conta] → manter `DO NOTHING`, detectar divergência por identidade e interromper a declaração de sucesso sem modificar a conta existente.
- [Credenciais de teste compartilhadas no repositório] → rotular como exclusivas de teste, limitar as contas ao ambiente de teste e instruir rotação antes de qualquer uso operacional.
- [A URL local apontar para banco diferente do Supabase de teste] → verificar ambiente e histórico Flyway antes da execução, confirmar a conexão sem imprimir credenciais e validar as identidades após a migration.

## Migration Plan

1. Gerar e conferir hashes BCrypt com o encoder da aplicação; criar V19 com os 13 inserts idempotentes.
2. Adicionar verificação automatizada das identidades, perfis e hashes.
3. Aplicar V19 ao Supabase com Flyway usando variáveis somente no processo; verificar o histórico e os 13 registros; repetir a verificação para confirmar que não há duplicatas nem sobrescritas.
4. Criar `usuarios_sistema.md` com os mesmos 13 registros e instruções de acesso.

**Rollback:** a migration é aditiva e não altera contas em conflito. Não remover contas automaticamente no rollback, pois não é possível distinguir com segurança as linhas inseridas por V19 de contas preexistentes após conflitos. Uma remoção, se necessária em ambiente de teste, deve identificar explicitamente as 13 matrículas e ocorrer apenas após conferência humana do ambiente e dos vínculos.
