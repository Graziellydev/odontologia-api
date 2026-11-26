import { LogOut, RefreshCw } from 'lucide-react';
import { useEffect, useEffectEvent, useRef, useState } from 'react';
import {
  buscarMetricasCoordenador,
  buscarSemestresCoordenador,
} from '../services/metricasCoordenadorService.js';
import DesempenhoHistoricoAcademico from './DesempenhoHistoricoAcademico.jsx';
import RelatoriosCoordenacao from './RelatoriosCoordenacao.jsx';
import './PainelCoordenador.css';

const numeroBR = new Intl.NumberFormat('pt-BR');
const INTERVALO_ATUALIZACAO_MS = 180_000;

function mensagemDeErro(erro, existemDadosAnteriores = false) {
  switch (erro.status) {
    case 400:
      return {
        titulo: 'Período inválido',
        descricao: 'Selecione um período letivo disponível e tente novamente.',
      };
    case 401:
      return {
        titulo: 'Sessão expirada',
        descricao: 'Entre novamente para continuar consultando os indicadores.',
      };
    case 403:
      return {
        titulo: 'Acesso não permitido',
        descricao: 'Você não tem permissão para visualizar estes indicadores.',
      };
    case 404:
      return {
        titulo: 'Período indisponível',
        descricao: 'O período selecionado não está mais disponível. Atualize a seleção.',
      };
    default:
      return existemDadosAnteriores
        ? {
            titulo: 'Não foi possível atualizar os indicadores',
            descricao: 'Ocorreu um problema ao carregar os dados. Tente novamente.',
          }
        : {
            titulo: '',
            descricao: 'Não foi possível carregar os indicadores. Tente novamente.',
          };
  }
}

function registrarErroTecnico(contexto, erro) {
  if (import.meta.env.DEV) {
    console.error(`[PainelCoordenador] ${contexto}`, erro);
  }
}

function formatarHora(valor) {
  return new Intl.DateTimeFormat('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(valor);
}

function GraficoBarras({ titulo, itens, campoRotulo, cor }) {
  const maiorTotal = Math.max(0, ...itens.map((item) => item.total));

  return (
    <section className="coordenador-chart" aria-labelledby={`chart-${campoRotulo}`}>
      <div className="coordenador-chart-heading">
        <h2 id={`chart-${campoRotulo}`}>{titulo}</h2>
        <span>{itens.length} {itens.length === 1 ? 'categoria' : 'categorias'}</span>
      </div>

      {itens.length === 0 ? (
        <p className="coordenador-chart-empty">Sem atendimentos concluídos nesta visualização.</p>
      ) : (
        <ul className="coordenador-chart-list">
          {itens.map((item) => {
            const rotulo = item[campoRotulo];
            const largura = maiorTotal > 0 ? (item.total / maiorTotal) * 100 : 0;

            return (
              <li className="coordenador-chart-row" key={rotulo}>
                <span className="coordenador-chart-label">{rotulo}</span>
                <span className="coordenador-chart-track" aria-hidden="true">
                  <span
                    className={`coordenador-chart-fill ${cor}`}
                    style={{ width: `${largura}%` }}
                  />
                </span>
                <span className="coordenador-chart-value">
                  {numeroBR.format(item.total)}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function PainelCoordenador({ usuario, onLogout }) {
  const [paginaAtual, setPaginaAtual] = useState('metricas');
  const [semestres, setSemestres] = useState([]);
  const [semestreSelecionado, setSemestreSelecionado] = useState(null);
  const [metricas, setMetricas] = useState(null);
  const [atualizadoEm, setAtualizadoEm] = useState(null);
  const [carregandoSemestres, setCarregandoSemestres] = useState(true);
  const [semestresCarregados, setSemestresCarregados] = useState(false);
  const [erroSemestres, setErroSemestres] = useState('');
  const [tentativaSemestres, setTentativaSemestres] = useState(0);
  const [carregandoMetricas, setCarregandoMetricas] = useState(false);
  const [erroMetricas, setErroMetricas] = useState('');
  const [tentativaMetricas, setTentativaMetricas] = useState(0);
  const [saindo, setSaindo] = useState(false);
  const [erroLogout, setErroLogout] = useState('');
  const requisicaoAtual = useRef(null);
  const requisicaoSemestres = useRef(null);
  const sequenciaRequisicao = useRef(0);
  const requisicaoPendente = useRef(false);
  const metricasValidasAnteriores = useRef(null);

  const iniciais = (usuario?.nome || 'Coordenador')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase();

  useEffect(() => {
    let ativo = true;
    requisicaoSemestres.current ??= buscarSemestresCoordenador();

    requisicaoSemestres.current
      .then((resposta) => {
        if (!ativo) return;
        const listaSemestres = Array.isArray(resposta?.semestres) ? resposta.semestres : [];
        setSemestres(listaSemestres);
        setSemestresCarregados(true);
        setErroSemestres('');
        setSemestreSelecionado(listaSemestres[0] || '');
        setCarregandoMetricas(listaSemestres.length > 0);
      })
      .catch((erro) => {
        if (ativo) {
          registrarErroTecnico('Falha ao carregar períodos', erro);
          setErroSemestres(mensagemDeErro(erro));
          setSemestresCarregados(false);
        }
      })
      .finally(() => {
        if (ativo) setCarregandoSemestres(false);
      });

    return () => {
      ativo = false;
    };
  }, [tentativaSemestres]);

  useEffect(() => {
    if (!semestresCarregados || semestres.length === 0 || semestreSelecionado === null) return undefined;

    const controlador = new AbortController();
    const sequencia = ++sequenciaRequisicao.current;
    requisicaoAtual.current = controlador;
    requisicaoPendente.current = true;

    buscarMetricasCoordenador(semestreSelecionado || undefined, {
      signal: controlador.signal,
    })
      .then((resposta) => {
        if (controlador.signal.aborted || sequencia !== sequenciaRequisicao.current) return;
        setMetricas(resposta);
        metricasValidasAnteriores.current = resposta;
        setAtualizadoEm(new Date());
        setErroMetricas('');
      })
      .catch((erro) => {
        if (controlador.signal.aborted || sequencia !== sequenciaRequisicao.current) return;
        registrarErroTecnico('Falha ao atualizar indicadores', erro);
        if (erro.status === 401 || erro.status === 403) {
          setMetricas(null);
          metricasValidasAnteriores.current = null;
          setAtualizadoEm(null);
        }
        setErroMetricas(mensagemDeErro(erro, Boolean(metricasValidasAnteriores.current)));
      })
      .finally(() => {
        if (sequencia === sequenciaRequisicao.current) {
          requisicaoAtual.current = null;
          requisicaoPendente.current = false;
          setCarregandoMetricas(false);
        }
      });

    return () => {
      controlador.abort();
      if (requisicaoAtual.current === controlador) {
        sequenciaRequisicao.current += 1;
        requisicaoAtual.current = null;
        requisicaoPendente.current = false;
      }
    };
  }, [semestreSelecionado, semestres, semestresCarregados, tentativaMetricas]);

  function solicitarAtualizacao() {
    if (requisicaoPendente.current || !semestresCarregados || semestres.length === 0) return;

    requisicaoPendente.current = true;
    setCarregandoMetricas(true);
    setErroMetricas('');
    setTentativaMetricas((tentativa) => tentativa + 1);
  }

  function alterarSemestre(evento) {
    const proximoSemestre = evento.target.value;
    if (proximoSemestre === semestreSelecionado) return;

    setCarregandoMetricas(true);
    setErroMetricas('');
    setSemestreSelecionado(proximoSemestre);
  }

  function tentarCarregarSemestres() {
    requisicaoSemestres.current = null;
    setCarregandoSemestres(true);
    setErroSemestres('');
    setSemestresCarregados(false);
    setTentativaSemestres((tentativa) => tentativa + 1);
  }

  async function encerrarSessao() {
    if (saindo) return;

    setSaindo(true);
    setErroLogout('');
    try {
      await onLogout();
    } catch (erro) {
      registrarErroTecnico('Falha ao encerrar sessão', erro);
      setErroLogout('Não foi possível encerrar a sessão. Verifique a conexão e tente novamente.');
    } finally {
      setSaindo(false);
    }
  }

  const solicitarAtualizacaoAutomatica = useEffectEvent(() => {
    solicitarAtualizacao();
  });

  useEffect(() => {
    let timer = null;

    function pararTimer() {
      if (timer !== null) {
        window.clearInterval(timer);
        timer = null;
      }
    }

    function iniciarTimer() {
      pararTimer();
      if (document.visibilityState !== 'visible') return;
      timer = window.setInterval(() => {
        if (document.visibilityState === 'visible') solicitarAtualizacaoAutomatica();
      }, INTERVALO_ATUALIZACAO_MS);
    }

    function aoMudarVisibilidade() {
      if (document.visibilityState === 'visible') {
        iniciarTimer();
        solicitarAtualizacaoAutomatica();
      } else {
        pararTimer();
      }
    }

    iniciarTimer();
    document.addEventListener('visibilitychange', aoMudarVisibilidade);
    return () => {
      pararTimer();
      document.removeEventListener('visibilitychange', aoMudarVisibilidade);
    };
  }, [semestresCarregados, semestres.length]);

  const filtroAtual = semestreSelecionado || null;
  const dadosDoFiltroAtual = metricas?.semestre === filtroAtual;
  const exibirMetricas = Boolean(metricas) && !carregandoMetricas;
  const periodoDosDados = metricas?.semestre || 'Todos os períodos';
  const periodoSolicitado = filtroAtual || 'Todos os períodos';

  return (
    <div className="coordenador-app">
      <aside className="coordenador-sidebar">
        <div className="coordenador-brand">
          <strong>UniCatólica</strong>
          <span>Clínica odontológica</span>
        </div>
        <nav className="coordenador-navigation" aria-label="Navegação da coordenação">
          <p className="coordenador-nav-caption">COORDENAÇÃO</p>
          <button
            type="button"
            className={`coordenador-nav-button${paginaAtual === 'metricas' ? ' coordenador-nav-item' : ''}`}
            aria-current={paginaAtual === 'metricas' ? 'page' : undefined}
            onClick={() => setPaginaAtual('metricas')}
          >
            Painel de métricas
          </button>
          <button
            type="button"
            className={`coordenador-nav-button${paginaAtual === 'desempenho' ? ' coordenador-nav-item' : ''}`}
            aria-current={paginaAtual === 'desempenho' ? 'page' : undefined}
            onClick={() => setPaginaAtual('desempenho')}
          >
            Desempenho acadêmico
          </button>
          <button type="button" className={`coordenador-nav-button${paginaAtual === 'demografico' ? ' coordenador-nav-item' : ''}`} aria-current={paginaAtual === 'demografico' ? 'page' : undefined} onClick={() => setPaginaAtual('demografico')}>Perfil demográfico</button>
          <button type="button" className={`coordenador-nav-button${paginaAtual === 'demanda-clinica' ? ' coordenador-nav-item' : ''}`} aria-current={paginaAtual === 'demanda-clinica' ? 'page' : undefined} onClick={() => setPaginaAtual('demanda-clinica')}>Demanda clínica</button>
        </nav>
        <div className="coordenador-sidebar-footer">Uso acadêmico</div>
      </aside>

      <main className="coordenador-main">
        <header className="coordenador-header">
          <span>Clínica acadêmica / Coordenação</span>
          <div className="coordenador-user">
            <span className="coordenador-avatar" aria-hidden="true">{iniciais}</span>
            <div className="coordenador-user-details">
              <strong>{usuario?.nome}</strong>
              <span>Coordenador</span>
            </div>
            <button
              type="button"
              className="coordenador-logout"
              onClick={encerrarSessao}
              disabled={saindo}
            >
              <LogOut size={15} aria-hidden="true" />
              {saindo ? 'Saindo...' : 'Sair'}
            </button>
          </div>
        </header>

        <div className="coordenador-content">
          {erroLogout && (
            <div className="coordenador-logout-error" role="alert">
              <span>{erroLogout}</span>
              <button type="button" onClick={encerrarSessao} disabled={saindo}>
                {saindo ? 'Saindo...' : 'Tentar novamente'}
              </button>
            </div>
          )}
          {paginaAtual === 'desempenho' ? (
            <DesempenhoHistoricoAcademico
              semestres={semestres}
              carregandoSemestres={carregandoSemestres}
              erroSemestres={erroSemestres}
              onRetrySemestres={tentarCarregarSemestres}
            />
          ) : paginaAtual === 'demografico' || paginaAtual === 'demanda-clinica' ? (
            <RelatoriosCoordenacao pagina={paginaAtual} semestres={semestres} />
          ) : (
            <>
          <div className="coordenador-page-heading">
            <div>
              <p className="coordenador-eyebrow">COORDENAÇÃO</p>
              <h1>Painel de Métricas Gerais</h1>
            </div>
            <p className="coordenador-last-updated" aria-live="polite">
              <span>Última atualização</span>
              <strong>{atualizadoEm ? formatarHora(atualizadoEm) : '—'}</strong>
            </p>
          </div>

          <section className="coordenador-toolbar" aria-label="Filtros do painel">
            <label className="coordenador-period-field" htmlFor="coordenador-periodo">
              <span>Período letivo</span>
              <select
                id="coordenador-periodo"
                value={semestreSelecionado ?? ''}
                disabled={carregandoSemestres || Boolean(erroSemestres) || semestres.length === 0}
                onChange={alterarSemestre}
              >
                <option value="">Todos os períodos</option>
                {semestres.map((semestre) => (
                  <option key={semestre} value={semestre}>{semestre}</option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="coordenador-refresh"
              onClick={solicitarAtualizacao}
              disabled={carregandoSemestres || Boolean(erroSemestres) || carregandoMetricas || semestres.length === 0}
            >
              {carregandoMetricas
                ? <span className="coordenador-inline-spinner" aria-hidden="true" />
                : <RefreshCw size={15} aria-hidden="true" />}
              {carregandoMetricas ? 'Atualizando...' : 'Atualizar'}
            </button>
          </section>

          {carregandoSemestres && (
            <p className="coordenador-load-note" role="status">Carregando períodos letivos...</p>
          )}

          {erroSemestres && !carregandoSemestres && (
            <div className="coordenador-load-error" role="alert">
              <div>
                {erroSemestres.titulo && <strong>{erroSemestres.titulo}</strong>}
                <p>{erroSemestres.descricao}</p>
              </div>
              <button type="button" className="coordenador-retry" onClick={tentarCarregarSemestres}>
                Tentar novamente
              </button>
            </div>
          )}

          {!carregandoSemestres && !erroSemestres && semestres.length === 0 && (
            <p className="coordenador-empty-state" role="status">
              Nenhum período letivo cadastrado.
            </p>
          )}

          {carregandoMetricas && (
            <p className="coordenador-load-note" role="status">
              <span className="coordenador-inline-spinner" aria-hidden="true" />
              {metricas ? 'Atualizando métricas...' : 'Carregando métricas...'}
            </p>
          )}

          {erroMetricas && !carregandoMetricas && (
            <div className="coordenador-load-error" role="alert">
              <div>
                {erroMetricas.titulo && <strong>{erroMetricas.titulo}</strong>}
                <p>{erroMetricas.descricao}</p>
                {metricas && (
                  <p className="coordenador-stale-note">
                    Exibindo os últimos dados disponíveis, atualizados às{' '}
                    {atualizadoEm ? formatarHora(atualizadoEm) : '—'}.
                    {!dadosDoFiltroAtual && (
                      <> Eles pertencem ao período {periodoDosDados}, não ao período selecionado ({periodoSolicitado}).</>
                    )}
                  </p>
                )}
              </div>
              <button type="button" className="coordenador-retry" onClick={solicitarAtualizacao}>
                Tentar novamente
              </button>
            </div>
          )}

          {exibirMetricas && metricas && (
            <>
              {metricas.totalAtendimentosConcluidos === 0 && (
                <p className="coordenador-zero-notice" role="status">
                  Nenhum atendimento concluído encontrado para {periodoDosDados}.
                </p>
              )}

              <section className="coordenador-summary" aria-label="Indicador principal">
                <div className="coordenador-summary-copy">
                  <span>Atendimentos concluídos</span>
                  <strong>{numeroBR.format(metricas.totalAtendimentosConcluidos)}</strong>
                </div>
                <span className="coordenador-summary-period">
                  {periodoDosDados}
                </span>
              </section>

              <section className="coordenador-chart-grid" aria-label="Gráficos de atendimentos">
                <GraficoBarras
                  titulo="Atendimentos por semestre"
                  itens={metricas.porSemestre}
                  campoRotulo="semestre"
                  cor="coordenador-bar-orange"
                />
                <GraficoBarras
                  titulo="Atendimentos por disciplina"
                  itens={metricas.porDisciplina}
                  campoRotulo="disciplina"
                  cor="coordenador-bar-teal"
                />
                <GraficoBarras
                  titulo="Atendimentos por especialidade"
                  itens={metricas.porEspecialidade}
                  campoRotulo="especialidade"
                  cor="coordenador-bar-blue"
                />
              </section>
            </>
          )}
            </>
          )}
        </div>
      </main>
    </div>
  );
}

export default PainelCoordenador;
