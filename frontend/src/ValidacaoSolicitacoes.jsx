import { useEffect, useMemo, useState } from "react";
import { RefreshCw, Search } from "lucide-react";
import { listarSolicitacoesAdministrativas } from "./services/solicitacaoService.js";
import "./ValidacaoSolicitacoes.css";

const SOLICITACOES_VAZIAS = Object.freeze([]);

const STATUS = [
  "PENDENTE_APROVACAO",
  "PENDENTE_FILA",
  "APROVADO_E_ALOCADO",
  "RECUSADO"
];

const STATUS_LABEL = {
  PENDENTE_APROVACAO: "Pendente de Aprovação",
  PENDENTE_FILA: "Pendente de Fila",
  APROVADO_E_ALOCADO: "Aprovado e Alocado",
  RECUSADO: "Recusado"
};

function formatarData(valor) {
  if (!valor) return "Não informada";

  const partes = String(valor).slice(0, 10).split("-");
  if (partes.length !== 3) return String(valor);

  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

function formatarDataHora(valor) {
  if (!valor) return "Data não informada";

  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return String(valor);

  return data.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  });
}

function mensagemErro(erro) {
  if (erro?.status === 401) return "É necessário estar autenticado para consultar as solicitações.";
  if (erro?.status === 403) return "Seu usuário não possui permissão de SECRETARIA.";
  if (!erro?.status) return "Não foi possível acessar a API. Confira se o backend está disponível.";
  return `Não foi possível consultar as solicitações (HTTP ${erro.status}).`;
}

export default function ValidacaoSolicitacoes({
  onAnalisar = () => {},
  onVisualizar = () => {},
  revisao = 0,
  onDadosAtualizados
}) {
  const [busca, setBusca] = useState("");
  const [statusSelecionado, setStatusSelecionado] = useState("");
  const [disciplinaSelecionada, setDisciplinaSelecionada] = useState("");
  const [tentativa, setTentativa] = useState(0);
  const [consulta, setConsulta] = useState(null);
  const chave = `${revisao}:${tentativa}`;
  const atual = consulta?.chave === chave ? consulta : null;
  const carregando = !atual;
  const erro = atual?.erro || "";
  const solicitacoes = atual?.dados || SOLICITACOES_VAZIAS;

  function carregarSolicitacoes() {
    setTentativa((valor) => valor + 1);
  }

  useEffect(() => {
    const controlador = new AbortController();
    async function carregar() {
      try {
        const dados = await listarSolicitacoesAdministrativas(controlador.signal);
        if (!controlador.signal.aborted) {
          setConsulta({ chave, dados, erro: "" });
          onDadosAtualizados?.(dados);
        }
      } catch (falha) {
        if (!controlador.signal.aborted) {
          setConsulta({ chave, dados: [], erro: mensagemErro(falha) });
        }
      }
    }
    carregar();
    return () => controlador.abort();
  }, [chave, onDadosAtualizados]);

  const disciplinas = useMemo(() => (
    [...new Set(solicitacoes.map((item) => item.disciplina).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, "pt-BR"))
  ), [solicitacoes]);

  const indicadores = useMemo(() => STATUS.map((status) => ({
    status,
    quantidade: solicitacoes.filter((item) => item.statusSolicitacao === status).length
  })), [solicitacoes]);

  const solicitacoesFiltradas = useMemo(() => {
    const termo = busca.trim().toLocaleLowerCase("pt-BR");

    return solicitacoes.filter((item) => {
      const textoBusca = [
        item.id,
        item.alunoNome,
        item.alunoIdentificacao
      ].filter(Boolean).join(" ").toLocaleLowerCase("pt-BR");

      return (
        (!termo || textoBusca.includes(termo)) &&
        (!statusSelecionado || item.statusSolicitacao === statusSelecionado) &&
        (!disciplinaSelecionada || item.disciplina === disciplinaSelecionada)
      );
    });
  }, [solicitacoes, busca, statusSelecionado, disciplinaSelecionada]);

  function acaoPara(item) {
    if (item.statusSolicitacao === "PENDENTE_APROVACAO") {
      return (
        <button
          className="solicitacoes-botao-tabela"
          type="button"
          onClick={() => onAnalisar(item)}
        >
          Analisar
        </button>
      );
    }

    return (
      <button
        className="solicitacoes-botao-tabela"
        type="button"
        onClick={() => onVisualizar(item)}
      >
        Visualizar
      </button>
    );
  }

  return (
    <main className="solicitacoes-conteudo">
      <div className="solicitacoes-cabecalho">
        <div>
          <span className="solicitacoes-subtitulo">ÁREA DA SECRETARIA</span>
          <h1>Solicitações dos alunos</h1>
          <p>Analise as necessidades acadêmicas e consulte os vínculos registrados.</p>
        </div>

        <button
          className="solicitacoes-botao-atualizar"
          type="button"
          onClick={() => carregarSolicitacoes()}
          disabled={carregando}
        >
          <RefreshCw size={15} />
          {carregando ? "Carregando..." : "Atualizar"}
        </button>
      </div>

      {erro ? (
        <div className="solicitacoes-alerta solicitacoes-alerta-erro" role="alert">
          <span>{erro}</span>
          <button type="button" onClick={() => carregarSolicitacoes()} disabled={carregando}>
            Tentar novamente
          </button>
        </div>
      ) : null}

      <section className="solicitacoes-indicadores" aria-label="Indicadores de solicitações">
        {indicadores.map((indicador, indice) => (
          <article
            className={`solicitacoes-indicador ${indice === 0 ? "solicitacoes-indicador-destaque" : ""}`}
            key={indicador.status}
          >
            <span>{STATUS_LABEL[indicador.status]}</span>
            <strong>{carregando || erro ? "—" : indicador.quantidade}</strong>
            <small>{erro ? "Consulta indisponível" : carregando ? "Aguardando consulta" : "Registros retornados pela API"}</small>
          </article>
        ))}
      </section>

      <section className="solicitacoes-filtros" aria-label="Filtros da listagem">
        <label className="solicitacoes-busca">
          <Search size={16} aria-hidden="true" />
          <input
            type="search"
            value={busca}
            onChange={(evento) => setBusca(evento.target.value)}
            placeholder="Buscar matrícula ou requisição"
            aria-label="Buscar por matrícula, nome ou requisição"
          />
        </label>

        <label>
          <span className="solicitacoes-rotulo-filtro">Status</span>
          <select
            value={statusSelecionado}
            onChange={(evento) => setStatusSelecionado(evento.target.value)}
            aria-label="Filtrar por status"
          >
            <option value="">Todos os status</option>
            {STATUS.map((status) => (
              <option value={status} key={status}>{STATUS_LABEL[status]}</option>
            ))}
          </select>
        </label>

        <label>
          <span className="solicitacoes-rotulo-filtro">Disciplina</span>
          <select
            value={disciplinaSelecionada}
            onChange={(evento) => setDisciplinaSelecionada(evento.target.value)}
            aria-label="Filtrar por disciplina"
          >
            <option value="">Todas as disciplinas</option>
            {disciplinas.map((disciplina) => (
              <option value={disciplina} key={disciplina}>{disciplina}</option>
            ))}
          </select>
        </label>
      </section>

      <section className="solicitacoes-tabela-container">
        <div className="solicitacoes-titulo-tabela">
          <h2>Requisições de pacientes</h2>
          <p>Perfil solicitado preservado em todas as etapas.</p>
        </div>

        {carregando ? (
          <div className="solicitacoes-estado" role="status">Carregando solicitações...</div>
        ) : erro ? (
          <div className="solicitacoes-estado">A consulta não foi concluída. Tente novamente.</div>
        ) : solicitacoesFiltradas.length === 0 ? (
          <div className="solicitacoes-estado">
            {solicitacoes.length === 0
              ? "Nenhuma solicitação foi retornada pela API."
              : "Nenhuma solicitação corresponde aos filtros informados."}
          </div>
        ) : (
          <div className="solicitacoes-tabela-scroll">
            <table>
              <thead>
                <tr>
                  <th>Requisição / aluno</th>
                  <th>Procedimento</th>
                  <th>Dente / classe</th>
                  <th>Disciplina</th>
                  <th>Turma / semestre</th>
                  <th>Paciente associado</th>
                  <th>Status</th>
                  <th>Ação</th>
                </tr>
              </thead>
              <tbody>
                {solicitacoesFiltradas.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>REQ-{item.id}</strong>
                      <span>{item.alunoNome || "Aluno não informado"}</span>
                      <span>{item.alunoIdentificacao || "Identificação não informada"}</span>
                      <span>Solicitada em {formatarDataHora(item.dataSolicitacao)}</span>
                    </td>
                    <td>{item.procedimento || "Não informado"}</td>
                    <td>{item.dente || "Não informado"} / {item.classe || "Não informada"}</td>
                    <td>{item.disciplina || "Não informada"}</td>
                    <td>
                      <strong>{item.turma || "Não informada"}</strong>
                      <span>Semestre atual: {item.semestreAtual ?? "Não informado"}</span>
                    </td>
                    <td>{item.pacienteAlocadoNome || "Sem paciente associado"}</td>
                    <td>
                      <span className={`solicitacoes-status solicitacoes-status-${String(item.statusSolicitacao || "").toLowerCase()}`}>
                        {STATUS_LABEL[item.statusSolicitacao] || item.statusSolicitacao || "Não informado"}
                      </span>
                      <small>Data desejada: {formatarData(item.dataDesejada)}</small>
                    </td>
                    <td>{acaoPara(item)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="solicitacoes-rodape-tabela">
          <span>{erro ? "Consulta indisponível" : carregando ? "Carregando registros..." : `${solicitacoesFiltradas.length} registro(s) exibido(s)`}</span>
          <span>Consulta administrativa</span>
        </div>
      </section>
    </main>
  );
}
