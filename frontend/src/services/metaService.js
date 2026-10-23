import axios from 'axios'

const API = 'http://localhost:8080'

export async function buscarPainelDeMetas(alunoId, semestre) {
  const params = semestre ? { semestre } : {}
  const { data } = await axios.get(`${API}/api/alunos/${alunoId}/metas`, { params })
  return data
}