import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Info, ListOrdered, RefreshCw } from "lucide-react";
import { api } from "./api.js";
import { recusarSolicitacao, aprovarSolicitacao, mensagemErroSolicitacao } from "./services/solicitacaoService.js";
import "./AnalisarSolicitacao.css";

const ROTULOS_STATUS = {
  PENDENTE_APROVACAO: "Pendente de Aprovação",
  PENDENTE_FILA: "Pendente de Fila",
  APROVADO_E_ALOCADO: "Aprovado e Alocado",
  RECUSADO: "Recusado"
};

function exibir(valor, indisponivel = "Não informado") {
  if (valor == null || String(valor).trim() === "") return indisponivel;
  return String(valor);
}

function formatarDataHora(valor) {
  if (!valor) return "Data não informada";
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return "Data indisponível";
  return data.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function formatarData(valor) {
  if (!valor) return "Não informada";
  const partes = String(valor).match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : "Data indisponível";
}

const ROTULOS_URGENCIA = {
  ALTA: "Alta",
  PNE: "PNE",
  MEDIA: "Média",
  "MÉDIA": "Média",
  BAIXA: "Baixa"
};

function mensagemErroFila(erro) {
  if (erro?.status === 401) return "É necessário estar autenticado para consultar a fila.";
  if (erro?.status === 403) return "Seu usuário não possui permissão para consultar a fila da secretaria.";
  if (erro?.status) return `Não foi possível consultar a fila (HTTP ${erro.status}). Tente novamente.`;
  return "Não foi possível acessar a fila. Verifique a conexão e tente novamente.";
}

function demandaCompativel(entrada, { procedimentoId, dente, classe }) {
  return entrada?.id != null
    && entrada.statusFila === "AGUARDANDO"
    && String(entrada.procedimentoId) === String(procedimentoId)
    && entrada.dente === dente
    && (classe == null || entrada.classe === classe);
}

async function consultarFilaCompativel(perfil, sinal) {
  if (!perfil.procedimentoId || !perfil.dente) {
    throw new Error("Procedimento e dente são necessários para consultar demandas compatíveis.");
  }
  if (sinal.aborted) return null;

  const filtros = new URLSearchParams({
    procedimentoId: String(perfil.procedimentoId),
    dente: perfil.dente
  });
  if (perfil.classe != null) filtros.set("classe", perfil.classe);

  const dados = await api(`/fila-espera?${filtros}`, { method: "GET", signal: sinal });
  if (sinal.aborted) return null;
  if (!Array.isArray(dados)) throw new Error("A API retornou uma resposta inválida para a fila.");

  // filter preserva a ordem oficial e impede selecionar entradas alocadas ou incompatíveis.
  const demandas = dados.filter((entrada) => demandaCompativel(entrada, perfil));
  let filaVazia = false;

  if (demandas.length === 0) {
    // A resposta filtrada não distingue fila vazia de ausência de compatibilidade.
    // Esta leitura serve apenas ao estado vazio; nunca alimenta a seleção.
    const filaGeral = await api("/fila-espera", { method: "GET", signal: sinal });
    if (sinal.aborted) return null;
    if (!Array.isArray(filaGeral)) throw new Error("A API retornou uma resposta inválida para a fila.");
    filaVazia = !filaGeral.some((entrada) => entrada?.statusFila === "AGUARDANDO");
  }

  return { demandas, filaVazia };
}

// A seleção mantém apenas o vínculo local com a entrada da fila, sem alocação.
export default function AnalisarSolicitacao({
  solicitacao, onVoltar, podeDecidir = false, onDecisaoRegistrada, onSolicitarAtualizacao
}) {
  const tituloRef = useRef(null);
  const modalRef = useRef(null);
  const enviandoRef = useRef(false);
  const [modal, setModal] = useState(null);
  const [justificativa, setJustificativa] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erroDecisao, setErroDecisao] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [consulta, setConsulta] = useState(null);
  const [selecao, setSelecao] = useState(null);
  const [tentativa, setTentativa] = useState(0);
  const solicitacaoId = solicitacao?.id;
  const procedimentoId = solicitacao?.procedimentoId;
  const dente = solicitacao?.dente?.trim() || "";
  const classe = solicitacao?.classe?.trim() || null;
  const status = solicitacao?.statusSolicitacao;
  const pendente = status === "PENDENTE_APROVACAO";
  const podeExecutar = podeDecidir && pendente && typeof onDecisaoRegistrada === "function";
  const podeConsultar = Boolean(solicitacaoId && procedimentoId && dente && status === "PENDENTE_APROVACAO");
  const chaveConsulta = JSON.stringify([solicitacaoId, procedimentoId, dente, classe, status, tentativa]);
  const consultaAtual = consulta?.chave === chaveConsulta ? consulta : null;
  const carregandoFila = podeConsultar && !consultaAtual;
  const erroFila = !podeConsultar
    ? "A consulta exige uma solicitação pendente de aprovação com procedimento e dente informados."
    : consultaAtual?.erro || "";
  const demandas = consultaAtual?.demandas || [];
  const filaVazia = consultaAtual?.filaVazia || false;
  const demandaSelecionada = selecao?.chave === chaveConsulta
    ? demandas.find((entrada) => entrada.id === selecao.filaEsperaId)
    : null;

  useEffect(() => {
    tituloRef.current?.focus();
  }, [solicitacaoId]);

  useEffect(() => {
    if (!podeConsultar) return undefined;
    const controlador = new AbortController();

    async function carregar() {
      try {
        const resultado = await consultarFilaCompativel(
          { procedimentoId, dente, classe },
          controlador.signal
        );
        if (!controlador.signal.aborted && resultado) {
          setConsulta({ chave: chaveConsulta, ...resultado, erro: "" });
        }
      } catch (erro) {
        // api.js transforma falhas do fetch; o sinal também identifica cancelamentos.
        if (!controlador.signal.aborted) {
          setConsulta({ chave: chaveConsulta, demandas: [], filaVazia: false, erro: mensagemErroFila(erro) });
        }
      }
    }

    carregar();
    return () => controlador.abort();
  }, [chaveConsulta, podeConsultar, procedimentoId, dente, classe]);

  function atualizarFila() {
    setSelecao(null);
    setTentativa((atual) => atual + 1);
  }

  useEffect(() => {
    if (!modal) return undefined;
    const anterior = document.activeElement;
    const tituloAoAbrir = tituloRef.current;
    const elemento = modalRef.current;
    elemento?.querySelector("textarea, button")?.focus();
    function teclado(evento) {
      if (evento.key === "Escape" && !enviandoRef.current) setModal(null);
      if (evento.key !== "Tab") return;
      const campos = [...elemento.querySelectorAll("button:not(:disabled), textarea:not(:disabled)")];
      if (!campos.length) { evento.preventDefault(); return; }
      const primeiro = campos[0], ultimo = campos[campos.length - 1];
      if (evento.shiftKey && document.activeElement === primeiro) { evento.preventDefault(); ultimo.focus(); }
      if (!evento.shiftKey && document.activeElement === ultimo) { evento.preventDefault(); primeiro.focus(); }
    }
    document.addEventListener("keydown", teclado);
    return () => {
      document.removeEventListener("keydown", teclado);
      if (anterior?.isConnected && !anterior.disabled) anterior.focus();
      else if (tituloAoAbrir?.isConnected) tituloAoAbrir.focus();
    };
  }, [modal]);

  function abrirRecusa() {
    if (!podeExecutar || enviandoRef.current) return;
    setJustificativa("");
    setErroDecisao("");
    setModal("recusar");
  }

  function abrirAprovacao() {
    if (!podeExecutar || !demandaSelecionada || enviandoRef.current) return;
    setErroDecisao("");
    setModal("aprovar");
  }

  async function confirmarDecisao(evento) {
    evento.preventDefault();
    if (!podeExecutar || enviandoRef.current || !modal) return;
    if (modal === "recusar" && (!justificativa.trim() || justificativa.trim().length > 5000)) {
      setErroDecisao("Informe uma justificativa com conteúdo e até 5.000 caracteres.");
      return;
    }
    if (modal === "aprovar" && !demandaSelecionada) {
      setErroDecisao("Selecione novamente uma demanda compatível antes de confirmar.");
      return;
    }
    enviandoRef.current = true;
    setEnviando(true);
    setErroDecisao("");
    setSucesso("");
    try {
      const resultado = modal === "recusar"
        ? await recusarSolicitacao(solicitacao, justificativa)
        : await aprovarSolicitacao(solicitacao, demandaSelecionada);
      const nomeConfirmado = modal === "aprovar" && resultado.pacienteAlocadoId === demandaSelecionada.pacienteId
        ? { pacienteAlocadoNome: demandaSelecionada.pacienteNome } : {};
      onDecisaoRegistrada({ ...resultado, ...nomeConfirmado });
      setModal(null);
      setSucesso(modal === "recusar" ? "Solicitação recusada com sucesso." : "Solicitação aprovada e paciente vinculado com sucesso.");
    } catch (erro) {
      setErroDecisao(mensagemErroSolicitacao(erro));
      if (modal === "aprovar" && [400, 404, 409, 422].includes(erro?.status)) {
        atualizarFila();
        setModal(null);
      }
      onSolicitarAtualizacao?.();
    } finally {
      enviandoRef.current = false;
      setEnviando(false);
    }
  }

  if (!solicitacao?.id) {
    return (
      <main className="conteudo analise-solicitacao" aria-labelledby="analise-titulo">
        <button type="button" className="analise-voltar" onClick={onVoltar}>
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar às solicitações
        </button>
        <h1 id="analise-titulo" ref={tituloRef} tabIndex={-1}>Solicitação indisponível</h1>
        <p role="alert">Volte à listagem e selecione uma solicitação para analisar.</p>
      </main>
    );
  }

  return (
    <main className="conteudo analise-solicitacao" aria-labelledby="analise-titulo">
      <button type="button" className="analise-voltar" disabled={enviando} onClick={onVoltar}>
        <ArrowLeft size={16} aria-hidden="true" />
        Voltar às solicitações
      </button>

      <div className="cabecalho-pagina">
        <div>
          <span className="analise-subtitulo">ÁREA DA SECRETARIA</span>
          <h1 id="analise-titulo" ref={tituloRef} tabIndex={-1}>Analisar e vincular paciente</h1>
          <p>REQ-{solicitacao.id} · solicitação recebida em {formatarDataHora(solicitacao.dataSolicitacao)}.</p>
        </div>
      </div>

      {sucesso && <p className="analise-feedback analise-feedback-sucesso" role="status">{sucesso}</p>}
      {erroDecisao && !modal && <p className="analise-feedback analise-feedback-erro" role="alert">{erroDecisao}</p>}
      <div className="analise-colunas">
        <div className="analise-coluna">
          <section className="analise-cartao" aria-labelledby="analise-necessidade">
            <h2 id="analise-necessidade">Necessidade do aluno</h2>
            <span className="analise-status" data-status={solicitacao.statusSolicitacao}>
              {ROTULOS_STATUS[solicitacao.statusSolicitacao] || exibir(solicitacao.statusSolicitacao)}
            </span>

            <dl className="analise-dados">
              <div><dt>Solicitante</dt><dd>{exibir(solicitacao.alunoNome)}</dd></div>
              <div><dt>Identificação</dt><dd>{exibir(solicitacao.alunoIdentificacao)}</dd></div>
              <div><dt>Turma</dt><dd>{exibir(solicitacao.turma, "Não informada")}</dd></div>
              <div><dt>Semestre atual do aluno</dt><dd>{exibir(solicitacao.semestreAtual)}</dd></div>
              <div><dt>Disciplina</dt><dd>{exibir(solicitacao.disciplina, "Não informada")}</dd></div>
              <div><dt>Procedimento</dt><dd>{exibir(solicitacao.procedimento)}</dd></div>
              <div><dt>Dente / classe</dt><dd>{exibir(solicitacao.dente)} / {exibir(solicitacao.classe, "Não informada")}</dd></div>
              <div><dt>Data da solicitação</dt><dd>{formatarDataHora(solicitacao.dataSolicitacao)}</dd></div>
              <div><dt>Data desejada</dt><dd>{formatarData(solicitacao.dataDesejada)}</dd></div>
              <div><dt>Saldo acadêmico</dt><dd className="analise-indisponivel">Não disponível nesta consulta.</dd></div>
            </dl>

            <p className="analise-observacao">
              <strong>Observação do aluno:</strong> {exibir(solicitacao.observacaoSecretaria, "Não disponível nesta consulta.")}
            </p>
          </section>

          {pendente && <section className="analise-cartao" aria-labelledby="analise-sem-compatibilidade">
            <h2 id="analise-sem-compatibilidade">Quando não houver compatibilidade</h2>
            <p className="analise-texto">
              Atualize a consulta quando novas demandas estiverem disponíveis. Consultar e selecionar não altera o status da solicitação.
            </p>
            <button
              type="button"
              className="botao-secundario"
              disabled
              aria-describedby="analise-pendente-indisponivel"
            >
              Marcar Pendente de Fila
            </button>
            <p className="analise-ajuda" id="analise-pendente-indisponivel">
              Esta opção ainda não está disponível.
            </p>
          </section>}
        </div>

        <div className="analise-coluna">
          {!pendente ? (
            <section className="analise-cartao" aria-labelledby="analise-decisao">
              <h2 id="analise-decisao">Decisão da solicitação</h2>
              <dl className="analise-dados">
                <div><dt>Status</dt><dd>{ROTULOS_STATUS[status] || exibir(status)}</dd></div>
                <div><dt>Resposta da secretaria</dt><dd className="analise-resposta">{exibir(solicitacao.respostaSecretaria, "Resposta não disponível nesta consulta.")}</dd></div>
                <div><dt>Data da análise</dt><dd>{formatarDataHora(solicitacao.dataAnalise)}</dd></div>
                <div><dt>Paciente vinculado</dt><dd>{exibir(solicitacao.pacienteAlocadoNome, solicitacao.pacienteAlocadoId ? `Paciente ${solicitacao.pacienteAlocadoId}` : "Sem paciente associado")}</dd></div>
              </dl>
              <p className="analise-ajuda">As ações de decisão não estão disponíveis para este status.</p>
            </section>
          ) : (
          <>
          <div
            className={`analise-aviso analise-aviso-fila ${!carregandoFila && !erroFila && demandas.length > 0 ? "analise-aviso-compativel" : ""}`}
            role="status"
          >
            <ListOrdered size={18} aria-hidden="true" />
            <div>
              <strong>
                {carregandoFila ? "Consultando demandas da fila..."
                  : erroFila ? "Consulta da fila indisponível"
                    : filaVazia ? "Nenhuma demanda disponível na fila"
                      : demandas.length === 0 ? "Nenhuma demanda compatível"
                        : `${demandas.length} demanda(s) compatível(is)`}
              </strong>
              <p>Perfil solicitado: {exibir(solicitacao.procedimento)} · dente {exibir(solicitacao.dente)} · classe {exibir(solicitacao.classe, "não informada")}.</p>
              <p>Ordem de prioridade oficial retornada pela fila de espera.</p>
            </div>
          </div>

          <section className="analise-cartao" aria-labelledby="analise-selecao">
            <div className="analise-cabecalho-fila">
              <h2 id="analise-selecao">Selecionar demanda da fila</h2>
              <button
                type="button"
                className="analise-atualizar-fila"
                onClick={atualizarFila}
                disabled={carregandoFila || !podeConsultar || enviando}
              >
                <RefreshCw size={14} aria-hidden="true" />
                Atualizar fila
              </button>
            </div>
            <p className="analise-texto">Sem reserva automática antes da aprovação.</p>

            <div className="analise-tabela-scroll" aria-busy={carregandoFila}>
              <table aria-label="Seleção de demandas da fila">
                <thead>
                  <tr>
                    <th scope="col">Paciente / demanda</th>
                    <th scope="col">Prioridade</th>
                    <th scope="col">Espera</th>
                    <th scope="col">Correspondência</th>
                  </tr>
                </thead>
                <tbody>
                  {carregandoFila ? (
                    <tr>
                      <td colSpan={4} className="analise-fila-indisponivel">
                        <span role="status">Carregando demandas compatíveis...</span>
                      </td>
                    </tr>
                  ) : erroFila ? (
                    <tr>
                      <td colSpan={4} className="analise-fila-indisponivel">
                        <div role="alert">
                          <strong>Não foi possível concluir a consulta.</strong>
                          <p>{erroFila}</p>
                          {podeConsultar && (
                            <button type="button" className="botao-secundario" onClick={atualizarFila}>
                              Tentar novamente
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : demandas.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="analise-fila-indisponivel">
                        <Info size={22} aria-hidden="true" />
                        <strong>
                          {filaVazia ? "A fila está sem demandas disponíveis."
                            : "Não há demandas compatíveis com esta solicitação."}
                        </strong>
                        <p>Atualize a fila para verificar novas demandas disponíveis.</p>
                      </td>
                    </tr>
                  ) : demandas.map((entrada) => (
                    <tr
                      key={entrada.id}
                      className={demandaSelecionada?.id === entrada.id ? "analise-demanda-selecionada" : ""}
                    >
                      <td>
                        <label className="analise-opcao-demanda">
                          <input
                            type="radio"
                            disabled={enviando}
                            name="demanda-compativel"
                            value={entrada.id}
                            checked={demandaSelecionada?.id === entrada.id}
                            onChange={() => setSelecao({ chave: chaveConsulta, filaEsperaId: entrada.id })}
                            aria-label={`Selecionar demanda D-${entrada.id} de ${exibir(entrada.pacienteNome)}`}
                          />
                          <span>
                            <strong>{exibir(entrada.pacienteNome)}</strong>
                            <small>D-{entrada.id}</small>
                          </span>
                        </label>
                      </td>
                      <td>
                        <span className="analise-prioridade" data-nivel={entrada.nivelUrgencia}>
                          {ROTULOS_URGENCIA[entrada.nivelUrgencia] || exibir(entrada.nivelUrgencia)}
                        </span>
                        {entrada.origem === "RETORNO" && <small className="analise-meta-demanda">Retorno</small>}
                        {entrada.prioridadeReagendamento === true && (
                          <small className="analise-meta-demanda">Reagendamento prioritário</small>
                        )}
                      </td>
                      <td>
                        {entrada.diasEspera != null ? `${entrada.diasEspera} dia(s)` : "Espera não informada"}
                        <small className="analise-meta-demanda">Entrada: {formatarDataHora(entrada.dataEntradaFila)}</small>
                      </td>
                      <td>
                        {exibir(entrada.procedimentoNome)} · {exibir(entrada.dente)} / {exibir(entrada.classe, "Não informada")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="analise-selecao-atual" role="status">
              {demandaSelecionada
                ? `Selecionada: ${exibir(demandaSelecionada.pacienteNome)} · D-${demandaSelecionada.id}`
                : "Nenhuma demanda selecionada."}
            </p>
            <p className="analise-ajuda">A seleção é local e não reserva o paciente.</p>

            <div className="analise-aviso analise-aviso-agendamento" role="note">
              <Info size={18} aria-hidden="true" />
              <div>
                <strong>Alocação não é agendamento</strong>
                <p>
                  A aprovação vinculará um paciente à solicitação na mesma operação.
                  Dia e turno serão informados pelo aluno posteriormente, em outra funcionalidade.
                </p>
              </div>
            </div>

            <div className="analise-acoes">
              <button type="button" className="botao-primario" disabled={!podeExecutar || !demandaSelecionada || enviando} onClick={abrirAprovacao}>
                Aprovar e Vincular
              </button>
              <button type="button" className="botao-secundario" disabled={!podeExecutar || enviando} onClick={abrirRecusa}>
                Recusar solicitação
              </button>
            </div>
            <p className="analise-ajuda" id="analise-acoes-indisponiveis">
              Aprovar exige uma demanda compatível selecionada. Recusar exige justificativa, sem necessidade de selecionar paciente.
            </p>
          </section>
          </>
          )}
        </div>
      </div>

      <section className="analise-cartao analise-conferencia" aria-labelledby="analise-conferencia">
        <h2 id="analise-conferencia">Conferência do vínculo</h2>
        <dl className="analise-dados">
          <div><dt>Solicitação</dt><dd>REQ-{solicitacao.id} · {exibir(solicitacao.alunoNome)}</dd></div>
          <div><dt>Paciente associado</dt><dd>{exibir(solicitacao.pacienteAlocadoNome, "Sem paciente associado")}</dd></div>
          <div>
            <dt>{status === "APROVADO_E_ALOCADO" ? "Demanda vinculada" : "Demanda selecionada"}</dt>
            <dd>{solicitacao.filaEsperaId && status === "APROVADO_E_ALOCADO"
              ? `D-${solicitacao.filaEsperaId}` : demandaSelecionada
              ? `D-${demandaSelecionada.id} · ${exibir(demandaSelecionada.pacienteNome)}`
              : "Nenhuma demanda selecionada."}</dd>
          </div>
          <div>
            <dt>Compatibilidade</dt>
            <dd className={demandaSelecionada ? "" : "analise-indisponivel"}>
              {!pendente ? "Decisão registrada; nova seleção indisponível." : demandaSelecionada
                ? classe == null
                  ? "Procedimento e dente correspondem. Solicitação sem restrição de classe."
                  : "Procedimento, dente e classe correspondem à solicitação."
                : "Aguardando consulta e seleção de uma demanda da fila."}
            </dd>
          </div>
        </dl>
      </section>
      {modal && (
        <div className="modal-fundo">
          <div className="modal analise-modal" role="dialog" aria-modal="true" aria-labelledby="decisao-titulo" ref={modalRef}>
            <h2 id="decisao-titulo">{modal === "recusar" ? "Confirmar recusa" : "Confirmar aprovação e vínculo"}</h2>
            <p>REQ-{solicitacao.id} · {exibir(solicitacao.alunoNome)}</p>
            <form onSubmit={confirmarDecisao}>
              {modal === "recusar" ? (
                <>
                  <label>
                    Justificativa da recusa *
                    <textarea required maxLength={5000} rows={5} value={justificativa} disabled={enviando}
                      onChange={(evento) => setJustificativa(evento.target.value)} />
                  </label>
                  <p className="analise-ajuda">Até 5.000 caracteres. A observação original do aluno será preservada.</p>
                </>
              ) : (
                <>
                  <dl className="analise-dados">
                    <div><dt>Paciente</dt><dd>{exibir(demandaSelecionada?.pacienteNome)}</dd></div>
                    <div><dt>Demanda</dt><dd>{demandaSelecionada ? `D-${demandaSelecionada.id}` : "Seleção indisponível"}</dd></div>
                    <div><dt>Procedimento</dt><dd>{exibir(solicitacao.procedimento)}</dd></div>
                    <div><dt>Dente / classe</dt><dd>{exibir(solicitacao.dente)} / {exibir(solicitacao.classe, "Não informada")}</dd></div>
                  </dl>
                  <p className="analise-ajuda">Esta confirmação aprova e vincula o paciente. Não cria agendamento nem define dia ou turno.</p>
                </>
              )}
              {erroDecisao && <p role="alert" className="analise-feedback analise-feedback-erro">{erroDecisao}</p>}
              <div className="modal-acoes">
                <button type="button" className="botao-secundario" disabled={enviando} onClick={() => setModal(null)}>Cancelar</button>
                <button type="submit" className="botao-primario" disabled={enviando || !podeExecutar || (modal === "aprovar" && !demandaSelecionada)}>
                  {enviando ? "Enviando..." : modal === "recusar" ? "Confirmar recusa" : "Confirmar aprovação"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}
