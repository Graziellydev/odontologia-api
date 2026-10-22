import { useEffect, useMemo, useState } from "react";

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

import { indicadoresIniciais } from "./data/pacientes";
import { listarFilaEspera } from "./service/FilaEsperaService";

const prioridade = {
  Alta: 3,
  Média: 2,
  Baixa: 1
};

function App() {
  const [pacientes, setPacientes] = useState([]);
  const [erroFila, setErroFila] = useState("");
  const [busca, setBusca] = useState("");
  const [demanda, setDemanda] = useState("");
  const [procedimento, setProcedimento] = useState("");
  const [urgencia, setUrgencia] = useState("");
  const [modalAberto, setModalAberto] = useState(false);
  const [menuAberto, setMenuAberto] = useState(false);
  const [aviso, setAviso] = useState("");
  useEffect(() => {
  async function carregarFila() {
    try {
      const dados = await listarFilaEspera();
      setPacientes(dados);
      setErroFila("");
    } catch (erro) {
      console.error("Erro ao carregar fila de espera:", erro);
      setErroFila("Não foi possível carregar a fila de espera.");
    }
  }

  carregarFila();
}, []);

  const [novoPaciente, setNovoPaciente] = useState({
    nome: "",
    cpf: "",
    demanda: "Endodontia",
    procedimento: "",
    classe: "",
    urgencia: "Média"
  });

  const procedimentos = [
    ...new Set(pacientes.map((p) => p.procedimento))
  ];

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
          prioridade[b.urgencia] - prioridade[a.urgencia];

        if (diferenca !== 0) return diferenca;

        return b.espera - a.espera;
      });
  }, [pacientes, busca, demanda, procedimento, urgencia]);

  function mostrarAviso(mensagem) {
    setAviso(mensagem);
  }

  function adicionarPaciente(evento) {
    evento.preventDefault();

    const hoje = new Date();

    const data = hoje.toLocaleDateString("pt-BR");

    const paciente = {
      ...novoPaciente,
      id: Date.now(),
      dataTriagem: data,
      espera: 0
    };

    setPacientes((anteriores) => [
      ...anteriores,
      paciente
    ]);

    setModalAberto(false);

    setNovoPaciente({
      nome: "",
      cpf: "",
      demanda: "Endodontia",
      procedimento: "",
      urgencia: "Média"
    });

    mostrarAviso("Paciente adicionado à fila de espera.");
  }

  function agendarPaciente(paciente) {
    mostrarAviso(
      `Agendamento de ${paciente.nome}: integração com a agenda será implementada posteriormente.`
    );
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
                    item.nome === "Fila de espera" ? "ativo" : ""
                  }`}
                  onClick={() => {
                    setMenuAberto(false);

                    if (item.nome !== "Fila de espera") {
                      mostrarAviso(
                        `A tela "${item.nome}" ainda será desenvolvida.`
                      );
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
              mostrarAviso("O login será integrado posteriormente.")
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
              Clínica acadêmica / Secretaria / Fila de espera
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

            <span className="avatar">CS</span>

            <strong>Camila Sousa</strong>
          </div>
        </header>

        <main className="conteudo">
          <div className="cabecalho-pagina">
            <div>
              <span className="subtitulo">FILA DE ESPERA</span>
              <h1>Fila de espera</h1>

              <p>
                48 pacientes aguardam uma vaga compatível
                com sua necessidade clínica.
              </p>
            </div>

            <button
              className="botao-primario"
              onClick={() => setModalAberto(true)}
            >
              <UserPlus size={16} />
              Nova triagem
            </button>
          </div>

          {erroFila && (
            <div className="sem-resultados" role="alert">
                {erroFila}
            </div>
            )}

          <section className="indicadores">
            {indicadoresIniciais.map((indicador) => (
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

                <p>{indicador.descricao}</p>
              </div>
            ))}
          </section>

          <section className="filtros">
            <div className="campo-busca">
              <Search size={17} />

              <input
                type="text"
                placeholder="Buscar paciente ou CPF"
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
                            <th style={{ textAlign: "center", paddingLeft: "0px", paddingRight: "300px" }}>Classe</th>
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
                      </td>

                      <td>{paciente.demanda}</td>

                      <td>{paciente.procedimento}</td>

                      <td>{paciente.classe || "-"}</td>

                      <td>{paciente.urgencia}</td>

                      <td>{paciente.espera} dias</td>

                      <td>
                        <button
                          className="botao-tabela"
                          onClick={() =>
                            agendarPaciente(paciente)
                          }
                        >
                          Agendar →
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {!erroFila && pacientesFiltrados.length === 0 && (
                <div className="sem-resultados">
                    Nenhum paciente encontrado na fila de espera.
                </div>
                )}
            </div>

            <div className="rodape-tabela">
              <span>
                {pacientesFiltrados.length} registros exibidos
                • dados demonstrativos
              </span>

              <span>Página 1 de 1</span>
            </div>
          </section>

          <section className="cartao-vaga">
            <div className="texto-vaga">
              <h2>Uma vaga surgiu em Endodontia</h2>

              <span>07/10/2026 • tarde • cadeira D5</span>

              <p>
                Ofereça primeiro a Roberto Lima e, depois,
                aos próximos pacientes do mesmo procedimento.
                A primeira aceitação recebe o agendamento.
              </p>
            </div>

            <div className="acoes-vaga">
              <span className="etiqueta-vaga">
                Vaga disponível
              </span>

              <button
                className="botao-primario"
                onClick={() =>
                  mostrarAviso(
                    "A chamada rápida será integrada ao fluxo de agendamento."
                  )
                }
              >
                <MessageCircle size={16} />
                Iniciar chamada rápida
              </button>
            </div>
          </section>

          <footer className="rodape-pagina">
            <span>
              UniCatólica • Clínica odontológica • Dados demonstrativos
            </span>

            <span>
              Uso institucional e restrito ao perfil autorizado
            </span>
          </footer>
        </main>
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

              <button onClick={() => setModalAberto(false)}>
                <X size={21} />
              </button>
            </div>

            <p className="modal-descricao">
              Cadastre um paciente na fila de espera.
            </p>

            <form onSubmit={adicionarPaciente}>
              <label>
                Nome completo
                <input
                  required
                  value={novoPaciente.nome}
                  onChange={(e) =>
                    setNovoPaciente({
                      ...novoPaciente,
                      nome: e.target.value
                    })
                  }
                />
              </label>

              <label>
                CPF
                <input
                  required
                  maxLength={14}
                  value={novoPaciente.cpf}
                  onChange={(e) =>
                    setNovoPaciente({
                      ...novoPaciente,
                      cpf: e.target.value
                    })
                  }
                />
              </label>

              <label>
                Demanda
                <select
                  value={novoPaciente.demanda}
                  onChange={(e) =>
                    setNovoPaciente({
                      ...novoPaciente,
                      demanda: e.target.value
                    })
                  }
                >
                  <option>Endodontia</option>
                  <option>Periodontia</option>
                  <option>Dentística</option>
                </select>
              </label>

              <label>
                Procedimento
                <input
                  required
                  value={novoPaciente.procedimento}
                  onChange={(e) =>
                    setNovoPaciente({
                      ...novoPaciente,
                      procedimento: e.target.value
                    })
                  }
                />
              </label>

              <label>
                Classe do dente
                <select
                    value={novoPaciente.classe}
                    onChange={(e) =>
                        setNovoPaciente({
                            ...novoPaciente,
                            classe: e.target.value
                    })
                }
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
                Urgência
                <select
                  value={novoPaciente.urgencia}
                  onChange={(e) =>
                    setNovoPaciente({
                      ...novoPaciente,
                      urgencia: e.target.value
                    })
                  }
                >
                  <option>Alta</option>
                  <option>Média</option>
                  <option>Baixa</option>
                </select>
              </label>

              <div className="modal-acoes">
                <button
                  type="button"
                  className="botao-secundario"
                  onClick={() => setModalAberto(false)}
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  className="botao-primario"
                >
                  Salvar triagem
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;