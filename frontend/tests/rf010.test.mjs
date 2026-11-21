import test, { beforeEach, afterEach, after } from "node:test";
import assert from "node:assert/strict";
import { JSDOM } from "jsdom";
import { createServer } from "vite";
import react from "@vitejs/plugin-react";
import { criarApiIsolada } from "./rf010-fixtures.mjs";

const dom = new JSDOM("<!doctype html><html><body></body></html>", { url: "http://127.0.0.1:5174" });
for (const nome of ["window", "document", "navigator", "HTMLElement", "HTMLTextAreaElement", "MutationObserver", "getComputedStyle", "localStorage"]) {
  Object.defineProperty(globalThis, nome, { configurable: true, value: dom.window[nome] });
}
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const React = await import("react");
const { render, screen, within, cleanup, fireEvent, waitFor, act } = await import("@testing-library/react");
const { default: userEvent } = await import("@testing-library/user-event");
const { default: axios } = await import("axios");
let modelo = criarApiIsolada();
globalThis.fetch = async (url, opcoes = {}) => {
  if (String(url) === "/api/auth/csrf") {
    dom.window.document.cookie = "XSRF-TOKEN=teste-csrf; path=/";
    return { ok: true, status: 204, json: async () => null, text: async () => "" };
  }
  assert.ok(String(url).startsWith("/api/"), "Requisição externa bloqueada");
  const r = await modelo.responder(opcoes.method || "GET", String(url), opcoes.body ? JSON.parse(opcoes.body) : undefined);
  return { ok: r.status >= 200 && r.status < 300, status: r.status,
    json: async () => structuredClone(r.data), text: async () => JSON.stringify(r.data) };
};
axios.defaults.adapter = async config => {
  const r = await modelo.responder(config.method.toUpperCase(), config.url, config.data && JSON.parse(config.data));
  const resposta = { status: r.status, data: r.data, config, headers: {} };
  if (r.status >= 400) throw Object.assign(new Error("Erro isolado"), { response: resposta });
  return resposta;
};
const vite = await createServer({
  configFile: false, envDir: false, plugins: [react()], appType: "custom",
  define: { "import.meta.env.VITE_API_URL": JSON.stringify("") },
  server: { middlewareMode: true, watch: null, hmr: false }
});
const { default: Analise } = await vite.ssrLoadModule("/src/AnalisarSolicitacao.jsx");
const { default: Shell } = await vite.ssrLoadModule("/src/FilaEspera.jsx");
const servico = await vite.ssrLoadModule("/src/services/solicitacaoService.js");
const usuario = { id: 7, nome: "Secretaria de teste", perfil: "SECRETARIA" };
let user;
beforeEach(() => { modelo = criarApiIsolada(); user = userEvent.setup({ document: dom.window.document }); });
afterEach(() => cleanup());
after(async () => { await vite.close(); dom.window.close(); });

function abrirAnalise(id = 1, autorizado = true) {
  function Tela() {
    const [pedido, setPedido] = React.useState(structuredClone(modelo.solicitacoes.find(s => s.id === id)));
    return React.createElement(Analise, {
      solicitacao: pedido, podeDecidir: autorizado, onVoltar() {},
      onDecisaoRegistrada: resposta => setPedido(atual => ({ ...atual, ...resposta }))
    });
  }
  render(React.createElement(Tela));
}
const patches = () => modelo.chamadas.filter(c => c.method === "PATCH");
async function abrirModalRecusa() {
  await user.click(screen.getByRole("button", { name: "Recusar solicitação" }));
  return screen.getByRole("dialog", { name: "Confirmar recusa" });
}
async function motivo(modal, texto = "Motivo controlado de teste") {
  await user.type(within(modal).getByRole("textbox", { name: "Justificativa da recusa *" }), texto);
}

test("T08 recusa sem paciente, preserva observacao e bloqueia nova decisao", async () => {
  modelo.fila = [];
  abrirAnalise();
  const modal = await abrirModalRecusa();
  await motivo(modal, "  Justificativa controlada  ");
  await user.click(within(modal).getByRole("button", { name: "Confirmar recusa" }));
  await screen.findByText("Solicitação recusada com sucesso.");
  assert.equal(patches().length, 1);
  assert.equal(patches()[0].method, "PATCH");
  assert.equal(patches()[0].caminho, "/api/solicitacoes-pacientes/1/recusar");
  assert.deepEqual(patches()[0].body, { observacaoSecretaria: "Justificativa controlada" });
  assert.equal(modelo.solicitacoes[0].observacaoSecretaria, "Observação original do aluno");
  assert.equal(screen.queryByRole("button", { name: "Recusar solicitação" }), null);
  assert.ok(within(document.querySelector(".analise-solicitacao")).getByText("Justificativa controlada"));
});

test("T08 justificativa ausente ou em branco nao chama PATCH", async () => {
  abrirAnalise();
  const modal = await abrirModalRecusa();
  fireEvent.submit(modal.querySelector("form"));
  await within(modal).findByRole("alert");
  await motivo(modal, "   ");
  fireEvent.submit(modal.querySelector("form"));
  assert.equal(patches().length, 0);
});

test("T08 limite de 5000 e contrato validado antes de enviar", async () => {
  await assert.rejects(servico.recusarSolicitacao(modelo.solicitacoes[0], "x".repeat(5001)));
  assert.equal(patches().length, 0);
});

test("T08 envio duplicado e bloqueado", async () => {
  let liberar;
  const espera = new Promise(resolve => { liberar = resolve; });
  modelo.antesDeResponder = (method) => method === "PATCH" ? espera : undefined;
  abrirAnalise();
  const modal = await abrirModalRecusa();
  await motivo(modal);
  const botao = within(modal).getByRole("button", { name: "Confirmar recusa" });
  fireEvent.click(botao);
  fireEvent.submit(modal.querySelector("form"));
  await waitFor(() => assert.equal(patches().length, 1));
  assert.equal(within(modal).getByRole("button", { name: "Enviando..." }).disabled, true);
  await act(async () => liberar());
  await screen.findByText("Solicitação recusada com sucesso.");
});

for (const status of [400, 401, 403, 404, 409, 503]) {
  test("T08 erro HTTP " + status + " sem falso sucesso nem segredo", async () => {
    modelo.falhar("PATCH", "/solicitacoes-pacientes/1/recusar", status, "senha=SEGREDO token=SEGREDO");
    abrirAnalise();
    const modal = await abrirModalRecusa();
    await motivo(modal);
    await user.click(within(modal).getByRole("button", { name: "Confirmar recusa" }));
    await within(modal).findByRole("alert");
    assert.equal(document.body.textContent.includes("SEGREDO"), false);
    assert.equal(modelo.solicitacoes[0].statusSolicitacao, "PENDENTE_APROVACAO");
    assert.equal(screen.queryByText("Solicitação recusada com sucesso."), null);
  });
}

test("T08 sem permissao nao habilita recusa", async () => {
  abrirAnalise(1, false);
  assert.equal(screen.getByRole("button", { name: "Recusar solicitação" }).disabled, true);
  assert.equal(patches().length, 0);
});

for (const [id, estado] of [[2, "RECUSADO"], [3, "APROVADO_E_ALOCADO"]]) {
  test("T08 solicitacao ja " + estado + " bloqueia recusa visual e chamada da API", async () => {
    abrirAnalise(id);
    assert.equal(screen.queryByRole("button", { name: "Recusar solicitação" }), null);
    assert.equal(screen.queryByRole("button", { name: "Aprovar e Vincular" }), null);
    const pedido = modelo.solicitacoes.find(s => s.id === id);
    await assert.rejects(servico.recusarSolicitacao(pedido, "Novo motivo"), erro => erro.status === 409);
    assert.equal(patches().length, 0);
    assert.equal(pedido.statusSolicitacao, estado);
  });
}

for (const [nome, valor] of [["nula", null], ["vazia", ""], ["espacos", "   "]]) {
  test("T08 justificativa " + nome + " bloqueada no cliente antes da API", async () => {
    await assert.rejects(servico.recusarSolicitacao(modelo.solicitacoes[0], valor), erro => erro.status === 400);
    assert.equal(patches().length, 0);
    assert.equal(modelo.solicitacoes[0].statusSolicitacao, "PENDENTE_APROVACAO");
  });
}

test("T08 shell atualiza tabela, indicadores e preserva filtros apos recusa", async () => {
  render(React.createElement(Shell, { usuario }));
  await user.click(screen.getByRole("button", { name: "Solicitações" }));
  await screen.findByRole("button", { name: "Analisar" });
  await user.type(screen.getByRole("searchbox"), "MAT-TESTE-A");
  await user.selectOptions(screen.getByRole("combobox", { name: "Filtrar por disciplina" }), "Dentística");
  await user.click(screen.getByRole("button", { name: "Analisar" }));
  const modal = await abrirModalRecusa();
  await motivo(modal);
  await user.click(within(modal).getByRole("button", { name: "Confirmar recusa" }));
  await screen.findByText("Solicitação recusada com sucesso.");
  await user.click(screen.getByRole("button", { name: "Voltar às solicitações" }));
  await waitFor(() => assert.equal(screen.getByRole("searchbox").value, "MAT-TESTE-A"));
  assert.equal(screen.getByRole("combobox", { name: "Filtrar por disciplina" }).value, "Dentística");
  const artigo = screen.getByText("Recusado", { selector: "article span" }).closest("article");
  await waitFor(() => assert.equal(artigo.querySelector("strong").textContent, "2"));
  const linhas = screen.getAllByRole("row");
  assert.ok(linhas.some(l => l.textContent.includes("REQ-1") && l.textContent.includes("Recusado")));
  const visualizar = screen.getAllByRole("button", { name: "Visualizar" })[0];
  await user.click(visualizar);
  assert.equal(screen.queryByRole("button", { name: "Recusar solicitação" }), null);
});

test("T09 sem selecao nao habilita aprovacao nem envia PATCH", async () => {
  abrirAnalise();
  await screen.findByRole("radio", { name: /demanda D-101/ });
  assert.equal(screen.getByRole("button", { name: "Aprovar e Vincular" }).disabled, true);
  await assert.rejects(servico.aprovarSolicitacao(modelo.solicitacoes[0], null));
  assert.equal(patches().length, 0);
});

test("T09 confirma paciente, usa somente aprovacao e impede repetir", async () => {
  abrirAnalise();
  const radio = await screen.findByRole("radio", { name: /demanda D-102/ });
  await user.click(radio);
  assert.equal(radio.checked, true);
  assert.ok(radio.closest("tr").classList.contains("analise-demanda-selecionada"));
  await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
  const modal = screen.getByRole("dialog", { name: "Confirmar aprovação e vínculo" });
  assert.ok(within(modal).getByText("Paciente de teste B"));
  await user.click(within(modal).getByRole("button", { name: "Confirmar aprovação" }));
  await screen.findByText("Solicitação aprovada e paciente vinculado com sucesso.");
  assert.equal(patches().length, 1);
  assert.equal(patches()[0].caminho, "/api/solicitacoes-pacientes/1/aprovar");
  assert.deepEqual(patches()[0].body, { filaEsperaId: 102 });
  assert.equal(modelo.fila.find(e => e.id === 102).statusFila, "ALOCADO");
  assert.equal(screen.queryByRole("button", { name: "Aprovar e Vincular" }), null);
  assert.equal(modelo.chamadas.some(c => /\/alocar$/.test(c.caminho)), false);
});

test("T09 conflito de alocacao limpa selecao e recarrega demandas", async () => {
  abrirAnalise();
  await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
  await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
  modelo.fila.find(e => e.id === 101).statusFila = "ALOCADO";
  await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Confirmar aprovação" }));
  await screen.findByRole("alert");
  await waitFor(() => assert.equal(screen.queryByRole("radio", { name: /demanda D-101/ }), null));
  assert.equal(screen.getByRole("button", { name: "Aprovar e Vincular" }).disabled, true);
  assert.equal(screen.queryByText("Solicitação aprovada e paciente vinculado com sucesso."), null);
  assert.equal(modelo.solicitacoes[0].statusSolicitacao, "PENDENTE_APROVACAO");
});

test("T09 incompatibilidade e pedido decidido sao rejeitados antes do PATCH", async () => {
  const pedido = modelo.solicitacoes[0];
  for (const campos of [{ procedimentoId: 8 }, { dente: "12" }, { classe: "II" }, { statusFila: "ALOCADO" }]) {
    await assert.rejects(servico.aprovarSolicitacao(pedido, { ...modelo.fila[0], ...campos }));
  }
  for (const status of ["RECUSADO", "APROVADO_E_ALOCADO", "PENDENTE_FILA"]) {
    await assert.rejects(servico.aprovarSolicitacao({ ...pedido, statusSolicitacao: status }, modelo.fila[0]));
  }
  assert.equal(patches().length, 0);
});

test("T09 aprovar duas vezes durante envio produz uma unica chamada", async () => {
  let liberar;
  const espera = new Promise(resolve => { liberar = resolve; });
  modelo.antesDeResponder = method => method === "PATCH" ? espera : undefined;
  abrirAnalise();
  await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
  await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
  const modal = screen.getByRole("dialog");
  fireEvent.submit(modal.querySelector("form"));
  fireEvent.submit(modal.querySelector("form"));
  await waitFor(() => assert.equal(patches().length, 1));
  await act(async () => liberar());
  await screen.findByText("Solicitação aprovada e paciente vinculado com sucesso.");
});

test("T09 atualiza lista, indicador e paciente vinculado no shell", async () => {
  render(React.createElement(Shell, { usuario }));
  await user.click(screen.getByRole("button", { name: "Solicitações" }));
  await user.click(await screen.findByRole("button", { name: "Analisar" }));
  await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
  await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
  await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Confirmar aprovação" }));
  await screen.findByText("Solicitação aprovada e paciente vinculado com sucesso.");
  await user.click(screen.getByRole("button", { name: "Voltar às solicitações" }));
  const artigo = screen.getByText("Aprovado e Alocado", { selector: "article span" }).closest("article");
  await waitFor(() => assert.equal(artigo.querySelector("strong").textContent, "2"));
  assert.ok(screen.getAllByRole("row").some(l => l.textContent.includes("REQ-1") && l.textContent.includes("Paciente de teste A")));
});

test("T07 selecao controlada preserva ordem e exclui alocados e incompativeis", async () => {
  abrirAnalise();
  const a = await screen.findByRole("radio", { name: /demanda D-101/ });
  const b = screen.getByRole("radio", { name: /demanda D-102/ });
  assert.deepEqual(screen.getAllByRole("radio").map(r => r.value), ["101", "102"]);
  assert.equal(screen.queryByRole("radio", { name: /demanda D-103/ }), null);
  assert.equal(screen.queryByRole("radio", { name: /demanda D-104/ }), null);
  await user.click(a); await user.click(b);
  assert.equal(a.checked, false); assert.equal(b.checked, true);
  assert.equal(document.querySelectorAll(".analise-demanda-selecionada").length, 1);
  assert.ok(document.querySelector(".analise-conferencia").textContent.includes("D-102"));
  assert.equal(patches().length, 0);
  await user.click(screen.getByRole("button", { name: "Atualizar fila" }));
  await waitFor(() => assert.equal(screen.getByRole("radio", { name: /demanda D-102/ }).checked, false));
});

test("T07 distingue fila vazia de ausencia de compatibilidade", async () => {
  modelo.fila = modelo.fila.filter(e => e.procedimentoId === 8);
  abrirAnalise();
  await screen.findByText("Não há demandas compatíveis com esta solicitação.");
  modelo.fila = [];
  await user.click(screen.getByRole("button", { name: "Atualizar fila" }));
  await screen.findByText("A fila está sem demandas disponíveis.");
  assert.equal(screen.queryByRole("radio"), null);
});

test("T07 filtros correspondem ao contrato do backend", async () => {
  abrirAnalise();
  await screen.findByRole("radio", { name: /demanda D-101/ });
  const chamada = modelo.chamadas.find(c => c.caminho.includes("/fila-espera?"));
  const params = new URL(chamada.caminho, "http://isolado").searchParams;
  assert.equal(params.get("procedimentoId"), "5"); assert.equal(params.get("dente"), "11"); assert.equal(params.get("classe"), "I");
});

test("T07 trocar solicitacao descarta selecao anterior", async () => {
  const props = { podeDecidir: false, onVoltar() {} };
  const view = render(React.createElement(Analise, { ...props, solicitacao: modelo.solicitacoes[0] }));
  await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
  const outra = { ...modelo.solicitacoes[0], id: 5, procedimentoId: 8 };
  view.rerender(React.createElement(Analise, { ...props, solicitacao: outra }));
  const radio = await screen.findByRole("radio", { name: /demanda D-104/ });
  assert.equal(radio.checked, false);
  assert.equal(screen.queryByRole("radio", { name: /demanda D-101/ }), null);
  assert.ok(document.querySelector(".analise-conferencia").textContent.includes("Nenhuma demanda selecionada."));
});

test("T10 falha administrativa nao apresenta zeros validos nem lista vazia", async () => {
  modelo.falhar("GET", "/solicitacoes-pacientes/administrativas", 503, "TOKEN-PRIVADO");
  render(React.createElement(Shell, { usuario }));
  await user.click(screen.getByRole("button", { name: "Solicitações" }));
  await screen.findByRole("alert");
  assert.deepEqual([...document.querySelectorAll(".solicitacoes-indicador strong")].map(e => e.textContent), ["—", "—", "—", "—"]);
  assert.equal(document.body.textContent.includes("TOKEN-PRIVADO"), false);
  assert.equal(screen.queryByText("Nenhuma solicitação foi retornada pela API."), null);
  await user.click(screen.getByRole("button", { name: "Tentar novamente" }));
  await screen.findByRole("button", { name: "Analisar" });
});

test("T10 PENDENTE_FILA continua visivel e sem operacoes indefinidas", async () => {
  abrirAnalise(4);
  assert.ok(within(document.querySelector(".analise-solicitacao")).getAllByText("Pendente de Fila").length > 0);
  assert.equal(screen.queryByRole("button", { name: "Aprovar e Vincular" }), null);
  assert.equal(screen.queryByRole("button", { name: "Recusar solicitação" }), null);
  assert.equal(patches().length, 0);
});

test("T10 cancelar recusa ou aprovacao nao grava", async () => {
  abrirAnalise();
  const modal = await abrirModalRecusa();
  await motivo(modal);
  await user.click(within(modal).getByRole("button", { name: "Cancelar" }));
  await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
  await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
  await user.keyboard("{Escape}");
  assert.equal(screen.queryByRole("dialog"), null);
  assert.equal(patches().length, 0);
});

for (const status of [401, 403, 503]) {
  test("T10 erro de aprovacao HTTP " + status + " nao mostra sucesso", async () => {
    modelo.falhar("PATCH", "/solicitacoes-pacientes/1/aprovar", status, "TOKEN-PRIVADO");
    abrirAnalise();
    await user.click(await screen.findByRole("radio", { name: /demanda D-101/ }));
    await user.click(screen.getByRole("button", { name: "Aprovar e Vincular" }));
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Confirmar aprovação" }));
    await screen.findByRole("alert");
    assert.equal(document.body.textContent.includes("TOKEN-PRIVADO"), false);
    assert.equal(screen.queryByText("Solicitação aprovada e paciente vinculado com sucesso."), null);
    assert.equal(modelo.solicitacoes[0].statusSolicitacao, "PENDENTE_APROVACAO");
  });
}

test("T08 falha de comunicacao nao mostra segredo ou sucesso", async () => {
  modelo.antesDeResponder = method => { if (method === "PATCH") throw new Error("senha=SEGREDO"); };
  abrirAnalise();
  const modal = await abrirModalRecusa(); await motivo(modal);
  await user.click(within(modal).getByRole("button", { name: "Confirmar recusa" }));
  await within(modal).findByRole("alert");
  assert.equal(document.body.textContent.includes("SEGREDO"), false);
  assert.equal(screen.queryByText("Solicitação recusada com sucesso."), null);
});

test("T08 resposta 200 sem confirmacao da decisao nao e sucesso", async () => {
  modelo.falhar("PATCH", "/solicitacoes-pacientes/1/recusar", 200, "resposta inesperada");
  abrirAnalise();
  const modal = await abrirModalRecusa(); await motivo(modal);
  await user.click(within(modal).getByRole("button", { name: "Confirmar recusa" }));
  await within(modal).findByRole("alert");
  assert.equal(screen.queryByText("Solicitação recusada com sucesso."), null);
});

test("T10 navegacao para fila pacientes e solicitacoes mantem shell unico", async () => {
  render(React.createElement(Shell, { usuario }));
  await user.click(screen.getByRole("button", { name: "Solicitações" }));
  await user.click(await screen.findByRole("button", { name: "Analisar" }));
  await user.click(screen.getByRole("button", { name: "Fila de espera" }));
  await screen.findByRole("heading", { name: "Fila de espera" });
  await user.click(screen.getByRole("button", { name: "Pacientes" }));
  assert.equal(document.querySelectorAll(".sidebar").length, 1);
  assert.equal(document.querySelectorAll(".topbar").length, 1);
  await user.click(screen.getByRole("button", { name: "Solicitações" }));
  await screen.findByRole("heading", { name: "Solicitações dos alunos" });
});
