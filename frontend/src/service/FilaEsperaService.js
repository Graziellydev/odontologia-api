import axios from "axios";

// O login oficial da equipe deverá fornecer a sessão/autenticação.
// Não há usuário, senha ou token temporário no front-end.
const api = axios.create({
  baseURL: "/api",
  timeout: 10000,
  withCredentials: true
});

export async function listarFilaEspera() {
  const resposta = await api.get("/fila-espera");

  return resposta.data.map((item) => {
    const urgencias = {
      ALTA: "Alta",
      MEDIA: "Média",
      "MÉDIA": "Média",
      BAIXA: "Baixa"
    };

    return {
      id: item.id,
      pacienteId: item.pacienteId,
      nome: item.pacienteNome ?? "",
      demanda: item.especialidade ?? "",
      procedimento: item.procedimentoNome ?? "",
      procedimentoId: item.procedimentoId,
      dente: item.dente ?? "",
      classe: item.classe ?? "",
      urgencia: urgencias[item.nivelUrgencia?.toUpperCase()] ?? item.nivelUrgencia ?? "",
      espera: item.diasEspera ?? 0,
      dataTriagem: item.dataEntradaFila
        ? new Date(item.dataEntradaFila).toLocaleDateString("pt-BR")
        : "",
      statusFila: item.statusFila ?? "",
      prioridadeReagendamento: item.prioridadeReagendamento ?? false,
      observacao: item.observacao ?? ""
    };
  });
}

export async function consultarTempoMedio() {
  const resposta = await api.get("/fila-espera/metricas/tempo-medio");
  return resposta.data;
}

export async function adicionarEntradaFila(dados) {
  const resposta = await api.post("/fila-espera", dados);
  return resposta.data;
}

export async function alocarPaciente(id) {
  const resposta = await api.patch(`/fila-espera/${id}/alocar`);
  return resposta.data;
}
