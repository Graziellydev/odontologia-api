import axios from 'axios'

export async function buscarPainelDeMetas(alunoId, semestre) {
  const params = semestre ? { semestre } : {}
  const { data } = await axios.get(`/api/alunos/${alunoId}/metas`, { 
    params,
    withCredentials: true 
  })
  return data
}