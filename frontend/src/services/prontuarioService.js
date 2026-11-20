import { api } from '../api.js'

export function buscarProntuarioDoPaciente(pacienteId) {
  return api(`/prontuarios/paciente/${encodeURIComponent(pacienteId)}`)
}

export function salvarEvolucaoProntuario(payload) {
  return api('/prontuarios', { method: 'POST', body: JSON.stringify(payload) })
}

export function adicionarAdendoProntuario(prontuarioId, texto) {
  return api(`/prontuarios/${encodeURIComponent(prontuarioId)}/adendos`, {
    method: 'POST',
    body: JSON.stringify({ texto }),
  })
}
