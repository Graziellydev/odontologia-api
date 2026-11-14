# Tasks

## 1. Grade semanal da agenda

- [x] 1.1 Definir DTO e endpoint de grade semanal com intervalo de datas e filtros de disciplina/disponibilidade; verificar com testes de controller que query params, autorização e formato da resposta correspondem ao contrato.
- [x] 1.2 Implementar leitura da grade a partir de cadeiras ativas, vagas e consultas ativas, incluindo combinações sem registro de vaga; verificar por testes de repositório os estados consulta, bloqueio e “Vaga livre”.
- [x] 1.3 Documentar o contrato da grade e seus estados visuais; verificar que todos os filtros e estados exibidos na interface estão descritos.

## 2. Operações transacionais de consulta

- [x] 2.1 Estender criação para selecionar cadeira e resolver ou criar a vaga no mesmo fluxo transacional da consulta e da fila; verificar por testes de serviço criação, reutilização, cadeira inativa/bloqueada e concorrência de `uk_vaga`.
- [x] 2.2 Confirmar remarcação com preservação da origem e cancelamento com retorno à fila sem apagar histórico; verificar por testes de integração alterações de status, `consulta_origem_id`, fila e liberação da vaga.
- [x] 2.3 Documentar os payloads e respostas de criação, remarcação e cancelamento, incluindo conflitos; verificar a documentação contra os testes de controller.

## 3. Interface oficial no Figma

- [x] 3.1 Integrar no topo de `AgendaSecretaria.jsx` a navegação semanal de sete dias, filtros, cartões por cadeira/turno e legenda da referência Figma; verificar visualmente os sete dias e os estados de consulta, livre e bloqueado.
- [x] 3.2 Integrar formulário de novo agendamento, resumo, remarcação e cancelamento aos serviços de consulta existentes; verificar os fluxos ponta a ponta, incluindo mensagens de erro e atualização da grade após cada operação.
- [x] 3.3 Manter o fluxo de faltas abaixo da nova agenda e verificar que a tela exibe ambos os conjuntos de operações sem regressão de navegação.
