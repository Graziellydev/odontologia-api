import { api } from '../api.js';

export function buscarSemestresCoordenador(opcoes = {}) {
  return api('/coordenador/metricas/semestres', opcoes);
}

export function buscarMetricasCoordenador(semestre, opcoes = {}) {
  const query = semestre ? `?semestre=${encodeURIComponent(semestre)}` : '';
  return api(`/coordenador/metricas${query}`, opcoes);
}