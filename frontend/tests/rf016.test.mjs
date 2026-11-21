import test, { after, afterEach, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://127.0.0.1:5174' });
for (const name of ['window', 'document', 'navigator', 'HTMLElement', 'MutationObserver', 'getComputedStyle', 'localStorage']) {
  Object.defineProperty(globalThis, name, { configurable: true, value: dom.window[name] });
}
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const React = await import('react');
const { render, screen, cleanup } = await import('@testing-library/react');
const { default: userEvent } = await import('@testing-library/user-event');
const vite = await createServer({
  configFile: false, envDir: false, plugins: [react()], appType: 'custom',
  define: { 'import.meta.env.VITE_API_URL': JSON.stringify('') },
  server: { middlewareMode: true, watch: null, hmr: false },
});
const { default: Login } = await vite.ssrLoadModule('/src/Login/Login.jsx');
let requests;
let failure;

globalThis.fetch = async (url, options = {}) => {
  const path = new URL(String(url), 'http://127.0.0.1').pathname;
  requests.push({ path, method: options.method || 'GET', body: options.body ? JSON.parse(options.body) : null, headers: options.headers });
  if (failure) return json(failure.status, failure.body);
  if (path === '/api/auth/login') return json(200, { id: 7, perfil: 'ALUNO', nome: 'Mariana' });
  if (path === '/api/auth/logout') return { ok: true, status: 204, json: async () => null, text: async () => '' };
  if (path === '/api/auth/esqueci-senha') return json(200, { mensagem: 'Se os dados corresponderem a uma conta elegível, enviaremos instruções.' });
  if (path === '/api/auth/redefinir-senha') return json(200, { mensagem: 'Senha redefinida.' });
  throw new Error(`Rota não prevista: ${options.method || 'GET'} ${path}`);
};
function json(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body, text: async () => JSON.stringify(body) };
}
let user;
beforeEach(() => { requests = []; failure = null; dom.window.document.cookie = 'XSRF-TOKEN=teste-csrf; path=/'; user = userEvent.setup({ document: dom.window.document }); });
afterEach(() => cleanup());
after(async () => { await vite.close(); dom.window.close(); });

test('login institucional aceita identificador e envia o token CSRF; falha mostra texto genérico', async () => {
  let autenticado;
  const { container, rerender } = render(React.createElement(Login, { onLoginSuccess: (u) => { autenticado = u; } }));
  assert.equal(screen.getAllByRole('tab').length, 3);
  assert.doesNotMatch(container.textContent, /Paciente|Professor/);
  await user.type(screen.getByPlaceholderText('mariana.costa@aluno.unicatolica.edu.br'), '2026001');
  await user.type(screen.getByLabelText('Senha'), 'senha-atual');
  await user.click(screen.getByRole('button', { name: /Entrar no sistema/ }));
  assert.deepEqual(requests[0].body, { identificador: '2026001', senha: 'senha-atual' });
  assert.equal(requests[0].headers['X-XSRF-TOKEN'], 'teste-csrf');
  assert.equal(autenticado.perfil, 'ALUNO');
  failure = { status: 401, body: { mensagem: 'Login ou senha inválidos' } };
  rerender(React.createElement(Login, { onLoginSuccess: () => {} }));
  await user.click(screen.getByRole('button', { name: /Entrar no sistema/ }));
  assert.ok(await screen.findByRole('alert'));
  assert.match(screen.getByRole('alert').textContent, /Login ou senha inválidos/);
});

test('solicitação de recuperação é neutra e a senha só é enviada após validações locais', async () => {
  render(React.createElement(Login, { onLoginSuccess: () => {} }));
  await user.click(screen.getByRole('button', { name: 'Esqueci minha senha?' }));
  await user.type(screen.getByPlaceholderText('Digite o e-mail ou matrícula'), '2026001');
  await user.click(screen.getByRole('button', { name: 'Enviar instruções' }));
  assert.deepEqual(requests[0].body, { identificador: '2026001' });
  assert.ok(await screen.findByLabelText('Código de 6 dígitos'));
  await user.type(screen.getByLabelText('Código de 6 dígitos'), '123456');
  const [senha, confirmacao] = document.querySelectorAll('input[type="password"]');
  await user.type(senha, 'senha-nova-com-12');
  await user.type(confirmacao, 'senha-diferente-12');
  await user.click(screen.getByRole('button', { name: 'Redefinir senha' }));
  assert.equal(requests.filter((r) => r.path.endsWith('/redefinir-senha')).length, 0);
  assert.match(screen.getByRole('alert').textContent, /senhas nÃ£o coincidem|senhas não coincidem/i);
  await user.clear(confirmacao);
  await user.type(confirmacao, 'senha-nova-com-12');
  await user.click(screen.getByRole('button', { name: 'Redefinir senha' }));
  const reset = requests.find((r) => r.path.endsWith('/redefinir-senha'));
  assert.deepEqual(reset.body, { identificador: '2026001', codigo: '123456', novaSenha: 'senha-nova-com-12' });
  assert.ok(await screen.findByText('Senha redefinida com sucesso.'));
});

test('API de login apresenta o mesmo erro genérico para falha de autenticação', async () => {
  failure = { status: 401, body: { mensagem: 'Login ou senha inválidos' } };
  render(React.createElement(Login, { onLoginSuccess: () => {} }));
  await user.type(screen.getByPlaceholderText('mariana.costa@aluno.unicatolica.edu.br'), 'nao-encontrado');
  await user.type(screen.getByLabelText('Senha'), 'incorreta');
  await user.click(screen.getByRole('button', { name: /Entrar no sistema/ }));
  assert.match((await screen.findByRole('alert')).textContent, /Login ou senha inválidos/);
});

test('bloqueia a seleção cruzada sem usar a aba como autoridade e encerra a sessão', async () => {
  let autenticado = false;
  render(React.createElement(Login, { onLoginSuccess: () => { autenticado = true; } }));
  await user.click(screen.getByRole('tab', { name: 'Secretaria' }));
  await user.type(screen.getByPlaceholderText('mariana.costa@aluno.unicatolica.edu.br'), '2026001');
  await user.type(screen.getByLabelText('Senha'), 'senha-atual');
  await user.click(screen.getByRole('button', { name: /Entrar no sistema/ }));
  assert.match((await screen.findByRole('alert')).textContent, /Acesso bloqueado/);
  assert.equal(autenticado, false);
  assert.ok(requests.some((r) => r.path === '/api/auth/logout'));
});
