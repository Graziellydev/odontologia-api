import { api } from '../api.js'

export function buscarPainelDeMetas(semestre) {
  const query = semestre ? `?semestre=${encodeURIComponent(semestre)}` : ''
  return api(`/metas/minhas${query}`)
}