// Na execução local, /api é encaminhado ao Spring Boot pelo proxy do Vite.
const BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

function lerCookie(nome) {
  if (typeof document === 'undefined') return '';
  const item = document.cookie.split(';').map((parte) => parte.trim())
    .find((parte) => parte.startsWith(`${nome}=`));
  return item ? decodeURIComponent(item.slice(nome.length + 1)) : '';
}

export async function obterHeadersCsrf() {
  let token = lerCookie('XSRF-TOKEN');
  if (!token) {
    const resposta = await fetch(`${BASE}/api/auth/csrf`, { credentials: 'include' });
    if (!resposta.ok) throw new Error('Não foi possível preparar a proteção da sessão.');
    token = lerCookie('XSRF-TOKEN');
  }
  return token ? { 'X-XSRF-TOKEN': token } : {};
}

export async function api(rota, opcoes = {}) {
  let resposta;
  const metodo = (opcoes.method || 'GET').toUpperCase();
  const headers = { ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}), ...opcoes.headers };
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(metodo)) {
    Object.assign(headers, await obterHeadersCsrf());
  }
  try {
    resposta = await fetch(`${BASE}/api${rota}`, {
      credentials: 'include', // envia a sessão de login em todas as requisições
      ...opcoes,
      headers,
    });
  } catch {
    throw new Error('Não foi possível acessar o backend. Confira se o Spring Boot está funcionando.');
  }
  if (!resposta.ok) {

    let detalhe;

    const texto = await resposta.text();
    try {
      const dados = JSON.parse(texto);
      detalhe = dados.detail || dados.message || dados.error || '';
    } catch { detalhe = texto.slice(0, 180); }
    const erro = new Error(`HTTP ${resposta.status}${detalhe ? ' — ' + detalhe : ''}`);
    erro.status = resposta.status;
    throw erro;
  }
  if (resposta.status === 204) return null;
  return resposta.json();
}
