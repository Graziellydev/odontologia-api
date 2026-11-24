import { api } from '../api.js';

export function buscarRelatorioSatisfacao({ semestre, pagina = 0, tamanho = 10 } = {}, opcoes = {}) {
  const parametros = new URLSearchParams();
  if (semestre) parametros.set('semestre', semestre);
  parametros.set('pagina', String(pagina));
  parametros.set('tamanho', String(tamanho));
  return api(`/coordenador/satisfacao?${parametros.toString()}`, opcoes);
}