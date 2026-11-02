import { api } from '../api.js'

export function buscarAtendimentosDeHoje() {
  return api('/retornos/meus-atendimentos')
}

export function registrarEncerramento(consultaId, necessitaRetorno, justificativa) {
  return api('/retornos', {
    method: 'POST',
    body: JSON.stringify({
      consultaId,
      necessitaRetorno,
      justificativa: justificativa || null,
    }),
  })
}