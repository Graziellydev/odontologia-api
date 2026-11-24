import test, { after, afterEach, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://127.0.0.1:5174' });
for (const name of ['window', 'document', 'navigator', 'HTMLElement', 'HTMLInputElement', 'HTMLSelectElement', 'MutationObserver', 'getComputedStyle', 'localStorage']) {
  Object.defineProperty(globalThis, name, { configurable: true, value: dom.window[name] });
}
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const React = await import('react');
const { render, screen, cleanup, fireEvent, act } = await import('@testing-library/react');
const vite = await createServer({
  configFile: false,
  envDir: false,
  plugins: [react()],
  appType: 'custom',
  define: { 'import.meta.env.VITE_API_URL': JSON.stringify('') },
  server: { middlewareMode: true, watch: null, hmr: false },
});
const { default: DesempenhoHistoricoAcademico } = await vite.ssrLoadModule(
  '/src/Coordenador/DesempenhoHistoricoAcademico.jsx',
);
const periodos = ['2026.2', '2026.1'];
let requisicoes;
let falha;
let respostaPersonalizada;
let erroDeRede;

globalThis.fetch = async (url, opcoes = {}) => {
  const destino = new URL(String(url), 'http://127.0.0.1');
  requisicoes.push({ url: destino, opcoes });
  if (erroDeRede) throw new Error('Falha de rede privada');
  if (falha) return json(falha.status, falha.body);
  if (respostaPersonalizada) return json(200, respostaPersonalizada);
  return json(200, {
    modalidade: 'TURMA',
    identificador: 'T1',
    semestre: '2026.1',
    nomeAluno: null,
    matriculaAluno: null,
    codigoTurma: 'T1',
    resumo: { exigidos: 5, concluidos: 2, pendentes: 3, progressoPercentual: 40 },
    historico: [{
      disciplina: 'Disciplina de teste',
      procedimento: 'Procedimento de teste',
      exigidos: 5,
      concluidos: 2,
      pendentes: 3,
      progressoPercentual: 40,
    }],
    pacientes: ['Paciente confidencial'],
    diagnosticos: ['Diagnóstico confidencial'],
    prontuarios: ['Texto clínico confidencial'],
    alunos: [{
      nomeAluno: 'Aluno de teste',
      matriculaAluno: 'A001',
      exigidos: 5,
      concluidos: 2,
      pendentes: 3,
      progressoPercentual: 40,
    }],
  });
};

function json(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  };
}

function renderTela() {
  return render(React.createElement(DesempenhoHistoricoAcademico, {
    semestres: periodos,
    carregandoSemestres: false,
    erroSemestres: null,
    onRetrySemestres: () => {},
  }));
}

beforeEach(() => {
  requisicoes = [];
  falha = null;
  respostaPersonalizada = null;
  erroDeRede = false;
});
afterEach(() => cleanup());
after(async () => { await vite.close(); dom.window.close(); });

test('consulta somente após confirmação e exibe histórico e comparativo reais da resposta', async () => {
  renderTela();

  assert.equal(requisicoes.length, 0);
  fireEvent.change(screen.getByLabelText('Visualização'), { target: { value: 'TURMA' } });
  fireEvent.change(screen.getByLabelText('Código da turma'), { target: { value: 'T1' } });
  fireEvent.change(screen.getByLabelText('Período'), { target: { value: '2026.1' } });
  assert.equal(requisicoes.length, 0);

  await act(async () => {
    fireEvent.submit(screen.getByRole('button', { name: 'Consultar' }).closest('form'));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  assert.equal(requisicoes.length, 1);
  assert.equal(requisicoes[0].url.pathname, '/api/coordenador/desempenho-historico');
  assert.equal(requisicoes[0].url.searchParams.get('modalidade'), 'TURMA');
  assert.equal(requisicoes[0].url.searchParams.get('identificador'), 'T1');
  assert.equal(requisicoes[0].url.searchParams.get('semestre'), '2026.1');
  assert.ok(screen.getByText('Procedimento de teste'));
  assert.ok(screen.getByText('Comparativo compacto – turma T1'));
  assert.ok(screen.getByText('Aluno de teste'));
  assert.ok(screen.getByText('Total da turma'));
  assert.doesNotMatch(
    document.body.textContent,
    /Paciente confidencial|Diagnóstico confidencial|Texto clínico confidencial/,
  );
});

test('resultado não encontrado mostra mensagem genérica e não detalhes internos', async () => {
  falha = { status: 404, body: { codigo: 'REGISTRO_NAO_ENCONTRADO', mensagem: 'Nenhum registro foi localizado.' } };
  renderTela();
  fireEvent.change(screen.getByLabelText('Matrícula do aluno'), { target: { value: 'NAO-EXISTE' } });
  await act(async () => {
    fireEvent.submit(screen.getByRole('button', { name: 'Consultar' }).closest('form'));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  assert.equal(screen.getByRole('alert').textContent, 'Nenhum registro foi localizado.');
});

test('sessão expirada e falha de rede exibem mensagens seguras', async () => {
  falha = { status: 401, body: { mensagem: 'detalhe interno' } };
  const { unmount } = renderTela();
  fireEvent.change(screen.getByLabelText('Matrícula do aluno'), { target: { value: 'A001' } });
  await act(async () => {
    fireEvent.submit(screen.getByRole('button', { name: 'Consultar' }).closest('form'));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  assert.equal(
    screen.getByRole('alert').textContent,
    'Sua sessão expirou. Entre novamente para continuar consultando.',
  );
  assert.doesNotMatch(screen.getByRole('alert').textContent, /detalhe interno/);

  unmount();
  falha = null;
  erroDeRede = true;
  renderTela();
  fireEvent.change(screen.getByLabelText('Matrícula do aluno'), { target: { value: 'A001' } });
  await act(async () => {
    fireEvent.submit(screen.getByRole('button', { name: 'Consultar' }).closest('form'));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  assert.equal(
    screen.getByRole('alert').textContent,
    'Não foi possível consultar os dados. Verifique a conexão e tente novamente.',
  );
});

test('mostra estado vazio para consulta válida sem metas ativas', async () => {
  respostaPersonalizada = {
    modalidade: 'ALUNO',
    identificador: 'A001',
    semestre: '2026.1',
    nomeAluno: 'Aluno de teste',
    matriculaAluno: 'A001',
    codigoTurma: null,
    resumo: { exigidos: 0, concluidos: 0, pendentes: 0, progressoPercentual: 0 },
    historico: [],
    alunos: [],
  };
  renderTela();
  fireEvent.change(screen.getByLabelText('Matrícula do aluno'), { target: { value: 'A001' } });
  await act(async () => {
    fireEvent.submit(screen.getByRole('button', { name: 'Consultar' }).closest('form'));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  assert.ok(screen.getByText('Não há metas acadêmicas ativas para esta consulta.'));
  assert.match(screen.getByText(/do exigido/).textContent, /^0%\s*do exigido$/);
  assert.equal(screen.queryByText('Comparativo compacto – turma'), null);
});

test('ignora uma resposta obsoleta após alterar filtros e consultar novamente', async () => {
  const pendencias = [];
  globalThis.fetch = async (url, opcoes = {}) => {
    const destino = new URL(String(url), 'http://127.0.0.1');
    requisicoes.push({ url: destino, opcoes });
    return new Promise((resolve) => pendencias.push(resolve));
  };
  renderTela();
  const formulario = screen.getByRole('button', { name: 'Consultar' }).closest('form');
  const matricula = screen.getByLabelText('Matrícula do aluno');
  fireEvent.change(matricula, { target: { value: 'A001' } });
  await act(async () => {
    fireEvent.submit(formulario);
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  fireEvent.change(matricula, { target: { value: 'A002' } });
  await act(async () => {
    fireEvent.submit(formulario);
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  assert.equal(requisicoes.length, 2);
  assert.equal(requisicoes[0].opcoes.signal.aborted, true);

  const respostaAluno = (nome, matriculaAluno, procedimento) => ({
    modalidade: 'ALUNO',
    identificador: matriculaAluno,
    semestre: '2026.1',
    nomeAluno: nome,
    matriculaAluno,
    codigoTurma: null,
    resumo: { exigidos: 1, concluidos: 1, pendentes: 0, progressoPercentual: 100 },
    historico: [{
      disciplina: 'Disciplina de teste',
      procedimento,
      exigidos: 1,
      concluidos: 1,
      pendentes: 0,
      progressoPercentual: 100,
    }],
    alunos: [],
  });
  await act(async () => {
    pendencias[1](json(200, respostaAluno('Aluno atual', 'A002', 'Registro atual')));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  await act(async () => {
    pendencias[0](json(200, respostaAluno('Aluno anterior', 'A001', 'Registro obsoleto')));
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  assert.ok(screen.getByText('Registro atual'));
  assert.equal(screen.queryByText('Registro obsoleto'), null);
  assert.equal(screen.queryByText('Aluno anterior'), null);
});
