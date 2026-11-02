import axios from 'axios'

const BASE_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

const faltasApi = axios.create({
  baseURL: `${BASE_URL}/api`,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** Registra a ausência da consulta e sua justificativa. */
export async function registrarFalta(consultaId, dados) {
  const resposta = await faltasApi.post(`/consultas/${encodeURIComponent(consultaId)}/faltas`, dados)
  return resposta.data
}

/** Cria uma nova consulta ligada à consulta original que teve falta. */
export async function reagendarFalta(origemId, dados) {
  const resposta = await faltasApi.post(
    `/consultas/${encodeURIComponent(origemId)}/reagendamentos`,
    dados,
  )
  return resposta.data
}

/** Lista consultas com falta; aceita filtros opcionais, como mes e quemFaltou. */
export async function listarFaltas(filtros = {}) {
  const params = Object.fromEntries(
    Object.entries(filtros).filter(([, valor]) => valor !== undefined && valor !== null && valor !== ''),
  )
  const resposta = await faltasApi.get('/consultas/faltas', { params })
  return Array.isArray(resposta.data) ? resposta.data : []
}

