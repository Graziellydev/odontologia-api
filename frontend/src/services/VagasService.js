import { api } from '../api.js'

const query = (params) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, value)
  })
  return search.toString() ? `?${search}` : ''
}

export const listarCadeirasAtivas = () => api('/cadeiras')
export const listarVagas = (filtros = {}) => api(`/vagas${query(filtros)}`)
export const buscarVaga = (id) => api(`/vagas/${encodeURIComponent(id)}`)
export const criarVaga = (dados) => api('/vagas', { method: 'POST', body: JSON.stringify(dados) })
export const bloquearVaga = (id, motivo) => api(`/vagas/${encodeURIComponent(id)}/bloqueio`, {
  method: 'PATCH', body: JSON.stringify({ motivo }),
})
export const bloquearTurno = (dados) => api('/vagas/bloqueio-turno', {
  method: 'PATCH', body: JSON.stringify(dados),
})
export const desbloquearVaga = (id) => api(`/vagas/${encodeURIComponent(id)}/desbloqueio`, { method: 'PATCH' })
export const removerVaga = (id) => api(`/vagas/${encodeURIComponent(id)}`, { method: 'DELETE' })
export const gerarTurnos = (dados) => api('/vagas/geracoes', {
  method: 'POST', body: JSON.stringify(dados),
})
export const listarGradeSemanal = (inicio, fim, filtros = {}) => api(`/vagas/grade${query({ inicio, fim, ...filtros })}`)
