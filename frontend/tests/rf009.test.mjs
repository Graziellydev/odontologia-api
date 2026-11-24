import test, { after, afterEach, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://127.0.0.1:5174' })
for (const name of ['window', 'document', 'navigator', 'HTMLElement', 'MutationObserver', 'getComputedStyle', 'localStorage']) {
  Object.defineProperty(globalThis, name, { configurable: true, value: dom.window[name] })
}
globalThis.IS_REACT_ACT_ENVIRONMENT = true
const React = await import('react')
const { render, screen, cleanup, within } = await import('@testing-library/react')
const { default: userEvent } = await import('@testing-library/user-event')
const vite = await createServer({
  configFile: false, envDir: false, plugins: [react()], appType: 'custom',
  define: { 'import.meta.env.VITE_API_URL': JSON.stringify('') },
  server: { middlewareMode: true, watch: null, hmr: false },
})
const { default: Workspace } = await vite.ssrLoadModule('/src/Secretaria/ComunicacaoWhatsapp.jsx')
const { default: AppointmentManager } = await vite.ssrLoadModule('/src/Secretaria/AgendamentosSecretaria.jsx')
let itens
let chamadas
let falharLista
let falharSimulacao
let notificacoesConsulta
let user

function resposta(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body, text: async () => JSON.stringify(body) }
}

globalThis.fetch = async (url, options = {}) => {
  const parsed = new URL(String(url), 'http://127.0.0.1')
  const method = (options.method || 'GET').toUpperCase()
  chamadas.push({ path: parsed.pathname, method, query: parsed.searchParams, body: options.body ? JSON.parse(options.body) : null })
  if (parsed.pathname === '/api/notificacoes' && method === 'GET') {
    if (falharLista) return resposta(503, { mensagem: 'Falha controlada' })
    let resultado = itens
    if (parsed.searchParams.has('tipo')) resultado = resultado.filter((item) => item.tipo === parsed.searchParams.get('tipo'))
    if (parsed.searchParams.has('status')) resultado = resultado.filter((item) => item.status === parsed.searchParams.get('status'))
    return resposta(200, structuredClone(resultado))
  }
  if (parsed.pathname === '/api/notificacoes/processar-lembretes-24h' && method === 'POST') {
    return resposta(200, { criadas: 2, existentes: 1 })
  }
  if (parsed.pathname === '/api/consultas' && method === 'GET') {
    return resposta(200, [{ id: 72, paciente: 'Paciente agenda', aluno: 'Aluno teste', alunoId: 5,
      data: '2026-11-24', turno: 'MANHA', procedimento: 'Restauração', status: 'AGENDADO' }])
  }
  if (parsed.pathname === '/api/consultas/fila-disponivel' || parsed.pathname === '/api/vagas/grade') {
    return resposta(200, [])
  }
  if (parsed.pathname === '/api/consultas/notificacoes') return resposta(200, structuredClone(notificacoesConsulta))
  if (parsed.pathname === '/api/webhooks/whatsapp' && method === 'POST') {
    if (falharSimulacao) return resposta(503, { mensagem: 'Falha de simulação controlada' })
    const item = itens.find((registro) => registro.telefonePaciente === chamadas.at(-1).body.telefone)
    if (item) {
      item.respostaRecebida = chamadas.at(-1).body.mensagem
      item.respondidaEm = '2026-11-23T11:00:00-03:00'
      item.status = 'CONFIRMADO_RESPOSTA'
    }
    return resposta(200, { resultado: 'CONFIRMADA' })
  }
  throw new Error(`Unexpected request ${method} ${parsed.pathname}`)
}

function notificacao(id, overrides = {}) {
  return {
    id, pacienteId: id, consultaId: id, paciente: `Paciente ${id}`, telefonePaciente: `1199999000${id}`,
    tipo: 'LEMBRETE_24H', conteudo: `Sua consulta ${id}, para confirmar: 1`, status: 'PENDENTE',
    criadaEm: '2026-11-23T08:00:00-03:00', linkWhatsapp: `https://wa.me/55119999000${id}?text=Sua%20consulta%20${id}`,
    dataConsulta: '2026-11-24', turno: 'MANHA', cadeira: 'C05', procedimento: 'Restauração',
    dente: '11', classe: 'I', respostaRecebida: null, respondidaEm: null,
    ...overrides,
  }
}

beforeEach(() => {
  itens = [notificacao(1), notificacao(2, { tipo: 'AVISO_CANCELAMENTO', status: 'ENVIADO' }),
    notificacao(3, { tipo: 'OFERTA_VAGA_DESISTENCIA' })]
  chamadas = []
  falharLista = false
  falharSimulacao = false
  notificacoesConsulta = [
    { id: 71, consultaId: 72, tipo: 'LEMBRETE_24H', status: 'PENDENTE', conteudo: 'Confirme a consulta.',
      criadaEm: '2026-11-23T08:00:00-03:00', telefonePaciente: '11999990001',
      linkWhatsapp: 'https://wa.me/5511999990001?text=Confirme' },
    { id: 72, consultaId: 72, tipo: 'AVISO_CANCELAMENTO', status: 'PENDENTE', conteudo: 'Consulta cancelada.',
      criadaEm: '2026-11-23T08:01:00-03:00', telefonePaciente: null, linkWhatsapp: null },
  ]
  dom.window.document.cookie = 'XSRF-TOKEN=rf009-test; path=/'
  user = userEvent.setup({ document: dom.window.document })
})
afterEach(() => cleanup())
after(async () => { await vite.close(); dom.window.close() })

test('carrega workspace, filtros, detalhes da vaga, link wa.me e simulação persistida', async () => {
  render(React.createElement(Workspace))
  assert.ok(await screen.findByText('Paciente 1'))
  assert.ok(screen.getByRole('heading', { name: 'Comunicação WhatsApp' }))
  assert.ok(screen.getByText('Mensagens automáticas'))
  assert.ok(screen.getByText('A primeira aceitação compatível ocupa a vaga'))
  const link = await screen.findByRole('link', { name: /Abrir no WhatsApp/ })
  assert.equal(link.getAttribute('href'), itens[0].linkWhatsapp)
  assert.equal(link.getAttribute('target'), '_blank')

  await user.click(screen.getByRole('button', { name: /Simular 1/ }))
  assert.deepEqual(chamadas.find((chamada) => chamada.path === '/api/webhooks/whatsapp').body,
    { telefone: itens[0].telefonePaciente, mensagem: '1' })
  assert.ok(await screen.findByText('Confirmação registrada na agenda'))
  assert.match(screen.getByRole('status').textContent, /não é uma mensagem real/)
})

test('filtra, pagina, prepara lembretes e mostra erro de consulta', async () => {
  itens = Array.from({ length: 7 }, (_, indice) => notificacao(indice + 1))
  render(React.createElement(Workspace))
  await screen.findByText('Paciente 1')
  assert.equal(screen.getAllByRole('row').length, 7)
  await user.click(screen.getByRole('button', { name: 'Próxima página' }))
  assert.ok(screen.getByText('Paciente 7'))
  assert.match(screen.getByText(/7 de 7 envios/).textContent, /7 de 7 envios/)

  await user.selectOptions(screen.getByLabelText('Tipo'), 'LEMBRETE_24H')
  assert.ok(chamadas.some((chamada) => chamada.path === '/api/notificacoes' && chamada.query.get('tipo') === 'LEMBRETE_24H'))
  await user.click(screen.getByRole('button', { name: /Gerar lembretes 24h/ }))
  assert.ok(await screen.findByRole('status'))
  assert.equal(chamadas.filter((chamada) => chamada.path === '/api/notificacoes/processar-lembretes-24h').length, 1)

  falharLista = true
  await user.click(screen.getByRole('button', { name: 'Atualizar mensagens' }))
  assert.match((await screen.findByRole('alert')).textContent, /HTTP 503/)
  assert.ok(within(screen.getByRole('table')).getByText(/Nenhuma notificação/))
})

test('desabilita ações sem telefone válido e permite seleção por linha', async () => {
  itens = [notificacao(1, { telefonePaciente: null, linkWhatsapp: null })]
  render(React.createElement(Workspace))
  const confirmar = await screen.findByRole('button', { name: /Simular 1/ })
  assert.equal(confirmar.disabled, true)
  assert.equal(document.querySelector('a.cw-whatsapp').getAttribute('href'), null)
  await user.click(screen.getByRole('row', { name: /Paciente 1/ }))
  assert.ok(screen.getByText(/Pronta para envio manual/))
})

test('mostra estado vazio quando não existem notificações', async () => {
  itens = []
  render(React.createElement(Workspace))
  assert.ok(await screen.findByText('Nenhuma notificação corresponde aos filtros.'))
  assert.ok(screen.getByText(/0 envios/))
  assert.ok(screen.getByText('Nenhuma chamada rápida registrada para o período filtrado.'))
})

test('modal de agendamentos compartilha link, desabilita telefone ausente, atualiza e exibe falha da simulação', async () => {
  render(React.createElement(AppointmentManager))
  await screen.findByText('Consulta #72')
  await user.click(screen.getByTitle('Ver notificações'))
  const dialog = await screen.findByRole('heading', { name: 'Notificações geradas' })
  assert.ok(dialog)
  const cards = document.querySelectorAll('.agendamento-mensagem')
  assert.equal(cards.length, 2)
  assert.equal(within(cards[0]).getByRole('link', { name: /Abrir no WhatsApp/ }).getAttribute('href'),
    'https://wa.me/5511999990001?text=Confirme')
  assert.equal(cards[1].querySelector('a').getAttribute('href'), null)
  assert.equal(within(cards[1]).getByRole('button', { name: /Simular 1/ }).disabled, true)

  await user.click(within(cards[0]).getByRole('button', { name: /Simular 1/ }))
  assert.deepEqual(chamadas.find((chamada) => chamada.path === '/api/webhooks/whatsapp').body,
    { telefone: '11999990001', mensagem: '1' })
  assert.ok(chamadas.some((chamada) => chamada.path === '/api/consultas/notificacoes'
    && chamada.query.get('consultaId') === '72'))

  falharSimulacao = true
  await user.click(within(document.querySelectorAll('.agendamento-mensagem')[0])
    .getByRole('button', { name: /Simular 2/ }))
  assert.match((await screen.findByRole('alert')).textContent, /Servico de agendamentos indisponivel/)
})
