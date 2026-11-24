# Proposal — RF013 Desempenho e Histórico Acadêmico

## Por quê
O coordenador precisa avaliar o desempenho acadêmico de estágio por aluno ou turma sem consultar manualmente prontuários em papel. O sistema já possui uma área de coordenação e métricas agregadas; esta entrega acrescenta uma consulta consolidada de procedimentos/atendimentos concluídos e pendentes por aluno ou turma, com período selecionável.

## O que muda
- Adicionar à área de Coordenação a tela **Desempenho e histórico acadêmico**, referenciada no protótipo anexado e no frame Figma **“Coordenação Desempenho e histórico acadêmico”**.
- Permitir alternar a visualização entre **Aluno** e **Turma**, informar matrícula ou código da turma e selecionar o período.
- Consultar dados reais do sistema e exibir resumo (exigidos, concluídos, pendentes e percentual de progresso), histórico consolidado por disciplina/tratamento e, na visão de turma, comparação compacta entre alunos e total da turma.
- Garantir que somente o perfil **COORDENADOR** tenha acesso à consulta, tanto na interface quanto no backend.
- Exibir estado de registro não encontrado, vazio, carregamento e falha, sem dados fictícios.

## Capabilities
### New
- `desempenho-historico-academico`

### Modified
- Área de coordenação / navegação, caso seja necessário adicionar a entrada de menu.

## Impacto esperado
- Frontend React: navegação da área Coordenador, nova tela e estilos/serviço.
- Backend Spring Boot: endpoint(s), DTO(s), service e repository/query para consolidar dados acadêmicos existentes. Reutilizar entidades, status, associações e regra de metas existentes; não duplicar regras de negócio.
- Banco: preferir consulta sobre estrutura existente. Criar migration somente se a investigação provar que falta dado persistido indispensável, e justificar no design.
- Segurança: manter sessão/CSRF existente e autorização por perfil COORDENADOR.
