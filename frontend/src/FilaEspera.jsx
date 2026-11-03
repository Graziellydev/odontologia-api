import { useCallback, useEffect, useMemo, useState } from "react";
import "./FilaEspera.css";
import CadastroPaciente from "./Pacientes/CadastroPaciente.jsx";
import ValidacaoSolicitacoes from "./ValidacaoSolicitacoes.jsx";
import AnalisarSolicitacao from "./AnalisarSolicitacao.jsx";


import {
  LayoutDashboard,
  Users,
  ListOrdered,
  CalendarDays,
  ClipboardList,
  MessageCircle,
  Bell,
  Search,
  UserPlus,
  LogOut,
  Menu,
  X
} from "lucide-react";

import { listarFilaEspera, adicionarEntradaFila, alocarPaciente } from "./service/FilaEsperaService";

const prioridade = {
  Alta: 3,
  Média: 2,
  Baixa: 1
};

function criarFormularioVazio() {
  return {
    pacienteId: "",
    procedimentoId: "",
    dente: "",
    classe: "",
    nivelUrgencia: "MEDIA",
    prioridadeReagendamento: false,
    observacao: ""
  };
}

function FilaEspera({ usuario, onLogout }) {
  const [pacientes, setPacientes] = useState([]);
  const [erroFila, setErroFila] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [busca, setBusca] = useState("");
  const [demanda, setDemanda] = useState("");
  const [procedimento, setProcedimento] = useState("");
  const [urgencia, setUrgencia] = useState("");
  const [modalAberto, setModalAberto] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [alocandoId, setAlocandoId] = useState(null);
  const [erroCadastro, setErroCadastro] = useState("");
  const [menuAberto, setMenuAberto] = useState(false);
  const [telaAtiva, setTelaAtiva] = useState("fila");
  const [solicitacaoSelecionada, setSolicitacaoSelecionada] = useState(null);
  const [revisaoSolicitacoes, setRevisaoSolicitacoes] = useState(0);
  const registrarDecisao = useCallback((resultado) => {
    setSolicitacaoSelecionada((atual) => atual?.id === resultado.id ? { ...atual, ...resultado } : atual);
    setRevisaoSolicitacoes((valor) => valor + 1);
  }, []);
  const sincronizarSelecionada = useCallback((dados) => {
    setSolicitacaoSelecionada((atual) => atual ? dados.find((item) => item.id === atual.id) || atual : atual);
  }, []);
  const [aviso, setAviso] = useState("");

  const [novoPaciente, setNovoPaciente] = useState(criarFormularioVazio());

  const procedimentos = [
    ...new Set(pacientes.map((p) => p.procedimento).filter(Boolean))
  ];

  const indicadores = useMemo(() => {
    const especialidades = [
      ...new Set([
        "Endodontia",
        "Periodontia",
        "Dentística",
        ...pacientes.map((p) => p.demanda).filter(Boolean)
      ])
    ];

    return especialidades.map((nome) => ({
      nome,
      quantidade: pacientes.filter((p) =>
        (p.demanda || "").toLocaleLowerCase("pt-BR") ===
        nome.toLocaleLowerCase("pt-BR")
      ).length
    }));
  }, [pacientes]);

  const pacientesFiltrados = useMemo(() => {
    return pacientes
      .filter((paciente) => {
        const termo = busca.toLowerCase().trim();

        const correspondeBusca =
          paciente.nome.toLowerCase().includes(termo);

        return (
          correspondeBusca &&
          (!demanda || paciente.demanda === demanda) &&
          (!procedimento ||
            paciente.procedimento === procedimento) &&
          (!urgencia || paciente.urgencia === urgencia)
        );
      })
      .sort((a, b) => {
        const diferenca =
          (prioridade[b.urgencia] ?? 0) - (prioridade[a.urgencia] ?? 0);

        if (diferenca !== 0) return diferenca;

        return b.espera - a.espera;
      });
  }, [pacientes, busca, demanda, procedimento, urgencia]);

  function mostrarAviso(mensagem) {
    setAviso(mensagem);
  }

  function mensagemErro(erro) {
    if (erro?.response?.status === 401) {
      return "A fila exige autenticação. O acesso ficará disponível após a integração com o login oficial.";
    }
    if (erro?.response?.status === 403) {
      return "Seu acesso não tem permissão para visualizar a fila de espera da secretaria.";
    }
    if (!erro?.response) {
      return "Não foi possível acessar a API. Confira se o Spring Boot está rodando na porta 8080.";
    }
    return `Não foi possível consultar a fila (HTTP ${erro.response.status}).`;
  }

  async function atualizarFila() {
    setCarregando(true);
    setErroFila("");
    try {
      const dados = await listarFilaEspera();
      setPacientes(dados);
    } catch (erro) {
      setPacientes([]);
      setErroFila(mensagemErro(erro));
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    let ativo = true;
    async function carregarFila() {
      setCarregando(true);
      try {
        const dados = await listarFilaEspera();
        if (ativo) {
          setPacientes(dados);
          setErroFila("");
        }
      } catch (erro) {
        if (ativo) {
          setPacientes([]);
          setErroFila(mensagemErro(erro));
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }
    carregarFila();
    return () => { ativo = false; };
  }, []);

  function mensagemFalhaGravacao(erro) {
    const status = erro?.response?.status;
    if (status === 401) return "Você precisa estar autenticado pelo login oficial para salvar uma triagem.";
    if (status === 403) return "O usuário autenticado não possui permissão de SECRETARIA.";
    if (status === 400 || status === 422) return "Dados rejeitados pela API. Confira o paciente, o procedimento, a urgência, o dente e a classe.";
    if (status === 404) return "Paciente ou procedimento não encontrado. Confira os dados do cadastro.";
    if (status === 409) return "O cadastro entrou em conflito com um registro existente.";
    if (status >= 500) return "O servidor não conseguiu salvar. Confira os logs do Spring Boot.";
    if (!erro?.response) return "Não foi possível conectar à API. Verifique se o Spring Boot está rodando.";
    return `Não foi possível salvar a triagem (HTTP ${status}).`;
  }

  function numeroId(valor) {
    const numero = Number(valor);
    return Number.isSafeInteger(numero) && numero > 0 ? numero : null;
  }

  async function adicionarPaciente(evento) {
    evento.preventDefault();
    if (salvando) return;
    setErroCadastro("");
    const pacienteId = numeroId(novoPaciente.pacienteId);
    const procedimentoId = numeroId(novoPaciente.procedimentoId);
    if (!pacienteId || !procedimentoId) {
      setErroCadastro("Informe o número de cadastro do paciente e o código de um procedimento já cadastrado.");
      return;
    }

    const dados = {
      pacienteId,
      procedimentoId,
      dente: novoPaciente.dente.trim() || null,
      classe: novoPaciente.classe || null,
      nivelUrgencia: novoPaciente.nivelUrgencia,
      prioridadeReagendamento: novoPaciente.prioridadeReagendamento,
      observacao: novoPaciente.observacao.trim() || null
    };

    setSalvando(true);
    try {
      const criado = await adicionarEntradaFila(dados);
      setModalAberto(false);
      setNovoPaciente(criarFormularioVazio());
      mostrarAviso(`Triagem salva no banco com sucesso${criado?.id ? ` (registro ${criado.id})` : ""}.`);
      await atualizarFila();
    } catch (erro) {
      console.error("Falha no cadastro da fila:", erro);
      setErroCadastro(mensagemFalhaGravacao(erro));
    } finally {
      setSalvando(false);
    }
  }

  async function agendarPaciente(paciente) {
    if (alocandoId !== null) return;
    if (!window.confirm(`Alocar ${paciente.nome} e atualizar o status da fila? Essa ação NÃO cria agendamento.`)) {
      return;
    }
    setAlocandoId(paciente.id);
    try {
      await alocarPaciente(paciente.id);
      mostrarAviso(`Paciente ${paciente.nome} alocado com sucesso.`);
      await atualizarFila();
    } catch (erro) {
      mostrarAviso(mensagemFalhaGravacao(erro));
    } finally {
      setAlocandoId(null);
    }
  }

  const itensMenu = [
    { nome: "Visão geral", icone: LayoutDashboard },
    { nome: "Pacientes", icone: Users },
    { nome: "Fila de espera", icone: ListOrdered },
    { nome: "Agenda", icone: CalendarDays },
    { nome: "Solicitações", icone: ClipboardList }
  ];

  return (
    <div className="aplicacao">
      <aside className={`sidebar ${menuAberto ? "aberto" : ""}`}>
        <div className="logo">
          <strong>UniCatólica</strong>
          <span>Clínica odontológica</span>
        </div>

        <div className="grupo-menu">
          <p className="titulo-menu">CLÍNICA</p>

          <nav>
            {itensMenu.map((item) => {
              const Icone = item.icone;

              return (
                <button
                  key={item.nome}
                  type="button"
                  className={`item-menu ${
                    (item.nome === "Fila de espera" && telaAtiva === "fila") ||
                    (item.nome === "Pacientes" && telaAtiva === "pacientes") ||
                    (item.nome === "Solicitações" && telaAtiva === "solicitacoes") ? "ativo" : ""
                  }`}
                  onClick={() => {
                    setMenuAberto(false);

                    if (item.nome === "Pacientes") {
                      setTelaAtiva("pacientes");
                    } else if (item.nome === "Fila de espera") {
                      setTelaAtiva("fila");
                    } else if (item.nome === "Solicitações") {
                      setSolicitacaoSelecionada(null);
                      setTelaAtiva("solicitacoes");
                    } else {
                      mostrarAviso(`A tela "${item.nome}" ainda será desenvolvida.`);
                    }
                  }}
                >
                  <Icone size={17} />
                  {item.nome}
                </button>
              );
            })}
          </nav>
        </div>

        <div className="grupo-menu">
          <p className="titulo-menu">ACOMPANHAMENTO</p>

          <button
            className="item-menu"
            onClick={() =>
              mostrarAviso("Módulo de comunicação em desenvolvimento.")
            }
          >
            <MessageCircle size={17} />
            Comunicação
          </button>
        </div>

        <div className="caixa-informacao">
          <strong>Clínica-escola</strong>
          <p>Do acolhimento ao próximo atendimento.</p>
        </div>

        <div className="rodape-menu">
          <p>Semestre letivo 2026.2</p>
          <p>Ajuda e orientações</p>

          <button
            onClick={() =>
              onLogout ? onLogout() : mostrarAviso("Não foi possível sair. Faça o login novamente.")
            }
          >
            <LogOut size={13} />
            Sair da conta
          </button>
        </div>
      </aside>

      {menuAberto && (
        <div
          className="fundo-menu"
          onClick={() => setMenuAberto(false)}
        />
      )}

      <div className="area-principal">
        <header className="topbar">
          <div className="topbar-esquerda">
            <button
              className="botao-menu-mobile"
              onClick={() => setMenuAberto(!menuAberto)}
            >
              <Menu size={21} />
            </button>

            <span>
              Clínica acadêmica / Secretaria / {telaAtiva === "pacientes" ? "Pacientes" : telaAtiva === "solicitacoes" ? "Solicitações" : "Fila de espera"}
              {telaAtiva === "solicitacoes" && solicitacaoSelecionada && ` / Analisar REQ-${solicitacaoSelecionada.id}`}
            </span>
          </div>

          <div className="usuario">
            <Bell size={17} />

            <span className="data-atual">
              {new Date().toLocaleDateString("pt-BR", {
                day: "2-digit",
                month: "short",
                year: "numeric"
              })}
            </span>

            <span className="avatar">{usuario?.nome?.charAt(0)?.toUpperCase() || "S"}</span>

            <strong>{usuario?.nome || "Secretaria"}</strong>
          </div>
        </header>

        {telaAtiva === "pacientes" ? (
          <CadastroPaciente
            nomeUsuario={usuario?.nome || "Secretaria"}
            onLogout={onLogout}
            integrado
          />
        ) : telaAtiva === "solicitacoes" ? (
          <>
            <div hidden={Boolean(solicitacaoSelecionada)}>
              <ValidacaoSolicitacoes
                revisao={revisaoSolicitacoes}
                onDadosAtualizados={sincronizarSelecionada}
                onAnalisar={(solicitacao) => {
                  setSolicitacaoSelecionada(solicitacao);
                  setAviso("");
                }}
                onVisualizar={(solicitacao) => {
                  setSolicitacaoSelecionada(solicitacao);
                  setAviso("");
                }}
              />
            </div>
            {solicitacaoSelecionada && (
              <AnalisarSolicitacao
                key={solicitacaoSelecionada.id}
                podeDecidir={usuario?.perfil === "SECRETARIA"}
                onDecisaoRegistrada={registrarDecisao}
                onSolicitarAtualizacao={() => setRevisaoSolicitacoes((valor) => valor + 1)}
                solicitacao={solicitacaoSelecionada}
                onVoltar={() => setSolicitacaoSelecionada(null)}
              />
            )}
          </>
        ) : (
        <main className="conteudo">
          <div className="cabecalho-pagina">
            <div>
              <span className="subtitulo">FILA DE ESPERA</span>
              <h1>Fila de espera</h1>

              <p>
                {erroFila
                  ? "Não foi possível consultar os pacientes no momento."
                  : carregando
                    ? "Carregando pacientes da fila de espera..."
                    : `${pacientes.length} registros retornados pela fila de espera.`}
              </p>
            </div>

            <button
              className="botao-primario"
              onClick={() => {
                setErroCadastro("");
                setModalAberto(true);
              }}
            >
              <UserPlus size={16} />
              Nova triagem
            </button>
          </div>

          <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 16 }}>
            <button
              className="botao-secundario"
              type="button"
              onClick={atualizarFila}
              disabled={carregando}
            >
              {carregando ? "Atualizando..." : "Atualizar fila"}
            </button>
          </div>

          {erroFila && (
            <div className="sem-resultados" role="alert" style={{ marginBottom: 16 }}>
              {erroFila}
            </div>
          )}

          {!erroFila && !carregando && (
          <section className="indicadores">
            {indicadores.map((indicador) => (
              <div
                key={indicador.nome}
                className={`cartao-indicador ${
                  indicador.nome === "Dentística"
                    ? "destaque"
                    : ""
                }`}
              >
                <span>{indicador.nome}</span>

                <strong>{indicador.quantidade}</strong>

                <p>Registros retornados pela API</p>
              </div>
            ))}
          </section>
          )}

          <section className="filtros">
            <div className="campo-busca">
              <Search size={17} />

              <input
                type="text"
                placeholder="Buscar paciente"
                value={busca}
                onChange={(evento) =>
                  setBusca(evento.target.value)
                }
              />
            </div>

            <select
              value={demanda}
              onChange={(evento) =>
                setDemanda(evento.target.value)
              }
            >
              <option value="">Todas as demandas</option>
              <option value="Endodontia">Endodontia</option>
              <option value="Periodontia">Periodontia</option>
              <option value="Dentística">Dentística</option>
            </select>

            <select
              value={procedimento}
              onChange={(evento) =>
                setProcedimento(evento.target.value)
              }
            >
              <option value="">Todos os procedimentos</option>

              {procedimentos.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>

            <select
              value={urgencia}
              onChange={(evento) =>
                setUrgencia(evento.target.value)
              }
            >
              <option value="">Todas as urgências</option>
              <option value="Alta">Alta</option>
              <option value="Média">Média</option>
              <option value="Baixa">Baixa</option>
            </select>
          </section>

          <div className="aviso-prioridade">
            <div className="aviso-titulo">
              <ListOrdered size={17} />

              <strong>
                Prioridade automática: maior urgência → maior
                tempo de espera
              </strong>
            </div>

            <p>
              Dentro da mesma urgência, a triagem mais antiga
              aparece primeiro. A fila não possui aluno fixo;
              o vínculo é definido no agendamento oficial.
            </p>
          </div>

          <section className="tabela-container">
            <div className="tabela-scroll">
                <table style={{ width: "100%", tableLayout: "fixed" }}>
                    <colgroup>
                        <col style={{ width: "19%" }} />
                        <col style={{ width: "14%" }} />
                        <col style={{ width: "15%" }} />
                        <col style={{ width: "8%" }} />
                        <col style={{ width: "14%" }} />
                        <col style={{ width: "12%" }} />
                        <col style={{ width: "18%" }} />
                    </colgroup>

                    <thead>
                        <tr>
                            <th>Paciente / triagem</th>
                            <th>Demanda</th>
                            <th>Procedimento</th>
                            <th className="coluna-classe">Classe</th>
                            <th>Urgência</th>
                            <th>Espera</th>
                            <th>Ação</th>
                        </tr>
                    </thead>

                <tbody>
                  {pacientesFiltrados.map((paciente) => (
                    <tr key={paciente.id}>
                      <td>
                        <strong>{paciente.nome}</strong>
                        <span>{paciente.dataTriagem}</span>
                        {paciente.origem === "RETORNO" && (
                          <>
                            <span className="etiqueta-retorno" title={paciente.observacao || ""}>
                              Retorno
                            </span>
                            {paciente.observacao && (
                              <span className="obs-retorno">{paciente.observacao}</span>
                            )}
                          </>
                        )}
                      </td>

                      <td>{paciente.demanda}</td>

                      <td>{paciente.procedimento}</td>

                      <td className="coluna-classe">{paciente.classe || "-"}</td>

                      <td>{paciente.urgencia}</td>

                      <td>{paciente.espera} dias</td>

                      <td>
                        <button
                          className="botao-tabela"
                          onClick={() =>
                            agendarPaciente(paciente)
                          }
                          disabled={alocandoId !== null || paciente.statusFila !== "AGUARDANDO"}
                          title="Atualiza o status na fila; não cria um agendamento"
                        >
                          {alocandoId === paciente.id ? "Alocando..." : paciente.statusFila !== "AGUARDANDO" ? "Alocado" : "Alocar →"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {!erroFila && !carregando && pacientesFiltrados.length === 0 && (
                <div className="sem-resultados">
                  Nenhum paciente encontrado na fila de espera.
                </div>
              )}
            </div>

            <div className="rodape-tabela">
              <span>
                {pacientesFiltrados.length} registros exibidos
                • consulta à API
              </span>

              <span>Página 1 de 1</span>
            </div>
          </section>

          <section className="cartao-vaga">
            <div className="texto-vaga">
              <h2>Agenda e vagas disponíveis</h2>
              <p>
                A consulta à fila está conectada à API. Vagas e agendamentos ainda
                dependem da integração com o módulo de agenda da equipe.
              </p>
            </div>
          </section>

          <footer className="rodape-pagina">
            <span>
              UniCatólica • Clínica odontológica • Consulta de fila via API
            </span>

            <span>
              Uso institucional e restrito ao perfil autorizado
            </span>
          </footer>
        </main>
        )}
      </div>

      {aviso && (
        <div className="notificacao" role="status">
          <span>{aviso}</span>

          <button onClick={() => setAviso("")}>
            <X size={17} />
          </button>
        </div>
      )}

      {modalAberto && (
        <div className="modal-fundo">
          <div className="modal">
            <div className="modal-cabecalho">
              <h2>Nova triagem</h2>
              <button type="button" disabled={salvando} onClick={() => setModalAberto(false)}>
                <X size={21} />
              </button>
            </div>

            <p className="modal-descricao">
              Informe o número do cadastro existente do paciente e o código do procedimento.
              Não é necessário informar ID de triagem.
            </p>

            <form onSubmit={adicionarPaciente}>
              <label>
                Número de cadastro do paciente *
                <input
                  type="number"
                  required
                  min="1"
                  step="1"
                  placeholder="Ex.: 1"
                  value={novoPaciente.pacienteId}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, pacienteId: e.target.value })}
                />
              </label>

              <label>
                Código do procedimento *
                <input
                  type="number"
                  required
                  min="1"
                  step="1"
                  placeholder="Ex.: 2"
                  value={novoPaciente.procedimentoId}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, procedimentoId: e.target.value })}
                />
              </label>

              <label>
                Dente (opcional)
                <input
                  maxLength={2}
                  placeholder="Ex.: 11"
                  value={novoPaciente.dente}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, dente: e.target.value })}
                />
              </label>

              <label>
                Classe do dente
                <select
                  value={novoPaciente.classe}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, classe: e.target.value })}
                >
                  <option value="">Não informada</option>
                  <option value="I">Classe I</option>
                  <option value="II">Classe II</option>
                  <option value="III">Classe III</option>
                  <option value="IV">Classe IV</option>
                  <option value="V">Classe V</option>
                  <option value="VI">Classe VI</option>
                </select>
              </label>

              <label>
                Urgência *
                <select
                  required
                  value={novoPaciente.nivelUrgencia}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, nivelUrgencia: e.target.value })}
                >
                  <option value="ALTA">Alta</option>
                  <option value="MEDIA">Média</option>
                  <option value="BAIXA">Baixa</option>
                </select>
              </label>

              <label style={{ display: "flex", flexDirection: "row", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  style={{ width: "auto" }}
                  checked={novoPaciente.prioridadeReagendamento}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, prioridadeReagendamento: e.target.checked })}
                />
                Prioridade de reagendamento
              </label>

              <label>
                Observação (opcional)
                <textarea
                  rows={3}
                  maxLength={5000}
                  value={novoPaciente.observacao}
                  onChange={(e) => setNovoPaciente({ ...novoPaciente, observacao: e.target.value })}
                />
              </label>

              {erroCadastro && (
                <p role="alert" style={{ color: "#b42318", fontWeight: 600 }}>
                  {erroCadastro}
                </p>
              )}

              <div className="modal-acoes">
                <button
                  type="button"
                  className="botao-secundario"
                  disabled={salvando}
                  onClick={() => setModalAberto(false)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="botao-primario"
                  disabled={salvando}
                >
                  {salvando ? "Salvando..." : "Salvar triagem"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default FilaEspera;