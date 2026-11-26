import { api, apiDownload } from '../api.js';

function query(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  const value = params.toString();
  return value ? `?${value}` : '';
}

export function buscarRelatorioDemografico(filters, options = {}) {
  return api(`/coordenador/relatorios/demografico${query(filters)}`, options);
}

export function buscarRelatorioDemandaClinica(filters, options = {}) {
  return api(`/coordenador/relatorios/demanda-clinica${query(filters)}`, options);
}

export function exportarRelatorioCoordenacao(tipo, filters, formato = 'CSV') {
  return apiDownload(`/coordenador/relatorios/${tipo}/exportar${query({ ...filters, formato })}`);
}
