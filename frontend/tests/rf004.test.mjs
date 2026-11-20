import test, { after, afterEach, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://127.0.0.1:5174' });
for (const name of ['window', 'document', 'navigator', 'HTMLElement', 'HTMLTextAreaElement', 'MutationObserver', 'getComputedStyle', 'localStorage']) {
  Object.defineProperty(globalThis, name, { configurable: true, value: dom.window[name] });
}
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const React = await import('react');
const { render, screen, within, cleanup } = await import('@testing-library/react');
const { default: userEvent } = await import('@testing-library/user-event');

const atendimento = {
  consultaId: 901, pacienteId: 71, procedimentoId: 12, cadeira: '03', paciente: 'Maria de Teste',
  procedimento: 'Restauração', dente: '36', classe: 'II', data: '2026-11-20', turno: 'TARDE', status: 'AGENDADO',
};
const dadosClinicos = () => ({
  paciente: { id: 71, nome: 'Maria de Teste', numeroProntuario: 'P-71', idade: 40 },
  anamnese: { queixaPrincipal: 'Sensibilidade' },
  odontograma: [{ id: 1, dente: '36', faceDente: 'O', condicaoDente: 'Restaurado', observacao: 'Acompanhar' }],
  evolucoes,
});
let consultas;
let falharHistorico = false;
let enviarEvolucao;
let enviarAdendo;
let evolucoes = [];

globalThis.fetch = async (url, options = {}) => {
  const path = new URL(String(url), 'http://127.0.0.1').pathname;
  const method = options.method || 'GET';
  if (path === '/api/retornos/meus-atendimentos') return json(200, consultas);
  if (path === '/api/prontuarios/paciente/71' && method === 'GET') {
    if (falharHistorico) return json(403, { detail: 'Acesso restrito: consulta ativa não vinculada no turno atual.' });
    return json(200, dadosClinicos());
  }
  if (path === '/api/prontuarios' && method === 'POST') {
    enviarEvolucao = JSON.parse(options.body);
    evolucoes = [{ id: 88, consultaId: 901, procedimento: atendimento.procedimento, evolucaoClinica: enviarEvolucao.evolucaoClinica, aluno: 'Aluno Teste', dataRegistro: '2026-11-20T16:30:00-03:00', adendos: [] }];
    return json(201, { id: 88, assinaturaDigital: 'v1;sha256=demo', atendimentoConcluido: enviarEvolucao.concluirAtendimento });
  }
  if (path === '/api/prontuarios/88/adendos' && method === 'POST') {
    enviarAdendo = JSON.parse(options.body);
    evolucoes = [{ ...evolucoes[0], adendos: [{ id: 98, texto: enviarAdendo.texto, criadoEm: '2026-11-20T16:35:00-03:00', autor: 'Aluno Teste' }] }];
    return json(201, { id: 98 });
  }
  throw new Error(`Requisição RF004 não prevista: ${method} ${path}`);
};

function json(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body, text: async () => JSON.stringify(body) };
}

const vite = await createServer({
  configFile: false, envDir: false, plugins: [react()], appType: 'custom',
  define: { 'import.meta.env.VITE_API_URL': JSON.stringify('') },
  server: { middlewareMode: true, watch: null, hmr: false },
});
const { default: ProntuarioDigital } = await vite.ssrLoadModule('/src/PainelAluno/ProntuarioDigital.jsx');
let user;
beforeEach(() => {
  consultas = [atendimento]; falharHistorico = false; enviarEvolucao = undefined; enviarAdendo = undefined; evolucoes = [];
  user = userEvent.setup({ document: dom.window.document });
});
afterEach(() => cleanup());
after(async () => { await vite.close(); dom.window.close(); });

test('lista consulta do aluno, registra evolução assinada e inclui adendo', async () => {
  render(React.createElement(ProntuarioDigital, { usuario: { nome: 'Aluno Teste', matriculaOuRegistro: '2026001' } }));
  await user.click(await screen.findByRole('button', { name: /Abrir prontuário/ }));
  const drawer = await screen.findByRole('dialog', { name: /Prontuário de Maria de Teste/ });
  assert.ok(within(drawer).getByText('Sensibilidade'));
  assert.match(drawer.textContent, /Aluno Teste/);
  await user.type(within(drawer).getByRole('textbox', { name: /Descrição clínica/ }), 'Restauração concluída sem intercorrências.');
  await user.click(within(drawer).getByRole('checkbox'));
  await user.click(within(drawer).getByRole('button', { name: /Salvar evolução e assinar/ }));
  assert.equal(enviarEvolucao.consultaId, 901);
  assert.equal(enviarEvolucao.evolucaoClinica, 'Restauração concluída sem intercorrências.');
  assert.equal(enviarEvolucao.concluirAtendimento, true);
  await user.click(within(drawer).getByRole('tab', { name: 'Evolução clínica' }));
  assert.ok(await within(drawer).findByText('REALIZADO'));
  await user.click(await within(drawer).findByRole('button', { name: /Adicionar Adendo/ }));
  const modal = await screen.findByRole('dialog', { name: 'Adicionar adendo' });
  await user.type(within(modal).getByRole('textbox', { name: /Texto do adendo/ }), 'Complemento ao registro.');
  await user.click(within(modal).getByRole('button', { name: 'Salvar adendo' }));
  assert.deepEqual(enviarAdendo, { texto: 'Complemento ao registro.' });
  assert.ok(await within(drawer).findByText(/Assinatura Digital Temporal Não Editável/));
});

test('mostra modal de bloqueio sem expor dados clínicos quando o histórico retorna 403', async () => {
  falharHistorico = true;
  render(React.createElement(ProntuarioDigital, { usuario: { nome: 'Aluno Teste', matricula: '2026001' } }));
  await user.click(await screen.findByRole('button', { name: /Abrir prontuário/ }));
  const bloqueio = await screen.findByRole('alertdialog', { name: 'Acesso restrito' });
  assert.match(bloqueio.textContent, /consulta ativa não vinculada/);
  assert.equal(screen.queryByText('Sensibilidade'), null);
});

test('exibe estado vazio quando não há consultas vinculadas', async () => {
  consultas = [];
  render(React.createElement(ProntuarioDigital, { usuario: { nome: 'Aluno Teste' } }));
  assert.ok(await screen.findByText(/Você não tem pacientes vinculados/));
});
