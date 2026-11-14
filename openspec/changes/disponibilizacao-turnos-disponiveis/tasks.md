# Tasks

## 1. Geração em lote

- [x] 1.1 Criar DTO e endpoint de geração por intervalo, retornando totais de combinações criadas e já existentes; verificar por testes de controller datas inválidas, intervalo invertido, autorização e contrato JSON.
- [x] 1.2 Implementar geração em lote para todas as datas, incluindo fins de semana, cadeiras ativas e os turnos MANHA/TARDE com `ON CONFLICT` da `uk_vaga`; verificar por testes de integração intervalo completo, cadeira inativa e repetição idempotente.
- [x] 1.3 Documentar payload, contadores e limites da geração em lote; verificar que a documentação corresponde ao endpoint e aos testes.

## 2. Grade semanal e operações de turno

- [x] 2.1 Implementar a leitura semanal para sete dias com estados consulta, bloqueado e vaga livre, inclusive quando a linha de vaga não existe; verificar por testes de repositório cada combinação de estado.
- [x] 2.2 Reutilizar as regras de bloqueio e exclusão de `gestao-vagas`, materializando a vaga quando necessário e recusando ações se houver qualquer consulta vinculada; verificar por testes de integração bloqueio, motivo, exclusão segura e histórico cancelado.
- [x] 2.3 Documentar o contrato da grade semanal e os estados apresentados; verificar que os exemplos incluem sábado e domingo e a conversão dos turnos para horários.

## 3. Interface da disponibilidade

- [x] 3.1 Implementar controles de período e geração em lote e exibir a resposta com quantidades criadas/existentes; verificar que repetir a ação atualiza os dados sem duplicar cartões.
- [x] 3.2 Implementar a grade de sete dias com rolagem horizontal acessível, cartões por cadeira e turno, horários e estados conforme a referência Figma; verificar os sete dias em telas estreitas e amplas.
- [x] 3.3 Integrar ações de bloqueio e exclusão com motivo e confirmação apropriada; verificar estados de sucesso, conflito por consulta vinculada e atualização da grade após a operação.
