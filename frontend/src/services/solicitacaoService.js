import { api } from "../api.js";

export async function listarSolicitacoesAdministrativas(sinal) {
  const dados = await api("/solicitacoes-pacientes/administrativas", { signal: sinal });
  if (!Array.isArray(dados)) throw new Error("Resposta administrativa inválida.");
  return dados;
}

function validarPendente(solicitacao) {
  if (!solicitacao?.id || solicitacao.statusSolicitacao !== "PENDENTE_APROVACAO") {
    throw Object.assign(new Error("Solicitação não elegível."), { status: 409 });
  }
}

function confirmarResposta(resposta, solicitacao, status) {
  if (resposta?.id !== solicitacao.id || resposta.statusSolicitacao !== status) {
    throw Object.assign(new Error("Decisão não confirmada pelo servidor."), { status: 502 });
  }
  return resposta;
}

export async function recusarSolicitacao(solicitacao, justificativa) {
  validarPendente(solicitacao);
  const motivo = justificativa?.trim();
  if (!motivo || motivo.length > 5000) {
    throw Object.assign(new Error("Justificativa inválida."), { status: 400 });
  }
  const resposta = await api(`/solicitacoes-pacientes/${solicitacao.id}/recusar`, {
    method: "PATCH",
    body: JSON.stringify({ observacaoSecretaria: motivo })
  });
  return confirmarResposta(resposta, solicitacao, "RECUSADO");
}

export async function aprovarSolicitacao(solicitacao, demanda) {
  validarPendente(solicitacao);
  if (!demanda?.id || demanda.statusFila !== "AGUARDANDO"
      || String(demanda.procedimentoId) !== String(solicitacao.procedimentoId)
      || demanda.dente !== solicitacao.dente
      || (solicitacao.classe != null && demanda.classe !== solicitacao.classe)) {
    throw Object.assign(new Error("Selecione uma demanda compatível disponível."), { status: 400 });
  }
  const resposta = await api(`/solicitacoes-pacientes/${solicitacao.id}/aprovar`, {
    method: "PATCH", body: JSON.stringify({ filaEsperaId: demanda.id })
  });
  confirmarResposta(resposta, solicitacao, "APROVADO_E_ALOCADO");
  if (resposta.filaEsperaId !== demanda.id || resposta.pacienteAlocadoId == null) {
    throw Object.assign(new Error("Vínculo não confirmado pelo servidor."), { status: 502 });
  }
  return resposta;
}

export function mensagemErroSolicitacao(erro) {
  if (erro?.status === 401) return "Sua sessão precisa ser autenticada novamente.";
  if (erro?.status === 403) return "Seu usuário não possui permissão de secretaria para esta operação.";
  if (erro?.status === 400 || erro?.status === 422) return "Os dados foram rejeitados. Confira a justificativa e os dados da solicitação.";
  if (erro?.status === 404) return "A solicitação ou a demanda não foi encontrada. Atualize os dados.";
  if (erro?.status === 409) return "Os dados foram alterados ou a demanda deixou de estar disponível. Atualize e confira antes de tentar novamente.";
  return "Não foi possível confirmar a operação. Consulte os dados atualizados antes de reenviar.";
}
