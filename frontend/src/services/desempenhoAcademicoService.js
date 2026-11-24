import { api } from '../api.js';

export function buscarDesempenhoAcademico({ modalidade, identificador, semestre }, opcoes = {}) {
  const parametros = new URLSearchParams({ modalidade, identificador, semestre });
  return api(`/coordenador/desempenho-historico?${parametros.toString()}`, opcoes);
}
