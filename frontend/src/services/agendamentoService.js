import { api } from '../api.js'

function query(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value)
  })
  return search.toString() ? `?${search}` : ''
}

export const listarAgendamentos = (filtros = {}) => api(`/consultas${query(filtros)}`)
export const listarFilaParaAgendamento = () => api('/consultas/fila-disponivel')
export const listarAlunosElegiveis = (filtros) => api(`/consultas/alunos-elegiveis${query(filtros)}`)
export const listarDisponibilidade = (filtros) => api(`/consultas/disponibilidade${query(filtros)}`)
export const listarNotificacoes = (consultaId) => api(`/consultas/notificacoes${query({ consultaId })}`)

export const criarAgendamento = (dados) => api('/consultas', {
  method: 'POST',
  body: JSON.stringify(dados),
})

export const cancelarAgendamento = (id) => api(`/consultas/${id}/cancelar`, { method: 'PATCH' })

export const remarcarAgendamento = (id, dados) => api(`/consultas/${id}/remarcar`, {
  method: 'POST',
  body: JSON.stringify(dados),
})

export const listarNotificacoesWhatsApp = (filtros = {}) => api('/notificacoes' + query(filtros))
export const processarLembretes24h = () => api('/notificacoes/processar-lembretes-24h', { method: 'POST' })
export const simularRespostaWhatsapp = (telefone, mensagem) => api('/webhooks/whatsapp', {
  method: 'POST',
  body: JSON.stringify({ telefone, mensagem }),
})
