// Na execução local, /api é encaminhado ao Spring Boot pelo proxy do Vite.
const BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
export async function api(rota, opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(`${BASE}/api${rota}`, {
      credentials: 'include', // envia a sessão de login em todas as requisições
      ...opcoes,
      headers: { ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}), ...opcoes.headers },
    });
  } catch {
    throw new Error('Não foi possível acessar o backend. Confira se o Spring Boot está funcionando.');
  }
  if (!resposta.ok) {

    let detalhe;

    const texto = await resposta.text();
    let detalhe;
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
