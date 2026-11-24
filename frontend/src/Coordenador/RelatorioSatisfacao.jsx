import { RefreshCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { buscarSemestresCoordenador } from '../services/metricasCoordenadorService.js';
import { buscarRelatorioSatisfacao } from '../services/satisfacaoCoordenadorService.js';
import './RelatorioSatisfacao.css';

const TAMANHO_PAGINA = 5;

const notaBR = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const inteiroBR = new Intl.NumberFormat('pt-BR');

function mensagemDeErro(erro) {
  switch (erro.status) {
    case 400:
      return {
        titulo: 'Período inválido',
        descricao: 'Selecione um período letivo disponível e tente novamente.',
      };
    case 401:
      return {
        titulo: 'Sessão expirada',
        descricao: 'Entre novamente para continuar consultando as avaliações.',
      };
    case 403:
      return {
        titulo: 'Acesso não permitido',
        descricao: 'Você não tem permissão para visualizar as avaliações.',
      };
    case 404:
      return {
        titulo: 'Período indisponível',
        descricao: 'O período selecionado não está mais disponível. Atualize a seleção.',
      };
    default:
      return {
        titulo: '',
        descricao: 'Não foi possível carregar as avaliações. Tente novamente.',
      };
  }
}

function formatarData(valor) {
  const data = valor ? new Date(valor) : null;
  return data && !Number.isNaN(data.getTime()) ? data.toLocaleDateString('pt-BR') : '—';
}

// Estrelas inteiras ou com meia estrela (a média é arredondada para o 0,5 mais próximo)
function Estrelas({ valor }) {
  const arredondado = Math.round(valor * 2) / 2;

  return (
    <span
      className="satisfacao-estrelas"
      role="img"
      aria-label={`${notaBR.format(valor)} de 5 estrelas`}
    >
      {[1, 2, 3, 4, 5].map((posicao) => {
        let preenchimento = 0;
        if (arredondado >= posicao) preenchimento = 100;
        else if (arredondado >= posicao - 0.5) preenchimento = 50;

        return (
          <span
            key={posicao}
            className="satisfacao-estrela"
            style={{ '--preenchimento': `${preenchimento}%` }}
            aria-hidden="true"
          >
            ★
          </span>
        );
      })}
    </span>
  );
}

function RelatorioSatisfacao() {
  const [semestres, setSemestres] = useState([]);
  const [semestre, setSemestre] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState({ chave: null, dados: null, erro: null });

  // Identifica a consulta atual: se o resultado guardado não for desta chave, ainda está carregando
  const chave = `${semestre}|${pagina}|${tentativa}`;

  useEffect(() => {
    let ativo = true;
    buscarSemestresCoordenador()
      .then((resposta) => {
        if (ativo) setSemestres(Array.isArray(resposta?.semestres) ? resposta.semestres : []);
      })
      .catch(() => {
        // Se a lista falhar, o filtro fica só com "Todos os períodos"
      });
    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    const controlador = new AbortController();

    buscarRelatorioSatisfacao(
      { semestre, pagina, tamanho: TAMANHO_PAGINA },
      { signal: controlador.signal },
    )
      .then((dados) => {
        if (!controlador.signal.aborted) setResultado({ chave, dados, erro: null });
      })
      .catch((erro) => {
        if (!controlador.signal.aborted) {
          setResultado({ chave, dados: null, erro: mensagemDeErro(erro) });
        }
      });

    return () => controlador.abort();
  }, [semestre, pagina, chave]);

  const carregando = resultado.chave !== chave;
  const dados = carregando ? null : resultado.dados;
  const erro = carregando ? null : resultado.erro;

  function alterarSemestre(evento) {
    setSemestre(evento.target.value);
    setPagina(0);
  }

  return (
    <>
      <div className="coordenador-page-heading">
        <div>
          <p className="coordenador-eyebrow">COORDENAÇÃO</p>
          <h1>Avaliações de Satisfação</h1>
        </div>
      </div>

      <section className="coordenador-toolbar" aria-label="Filtros do relatório">
        <label className="coordenador-period-field" htmlFor="satisfacao-periodo">
          <span>Período letivo</span>
          <select id="satisfacao-periodo" value={semestre} onChange={alterarSemestre}>
            <option value="">Todos os períodos</option>
            {semestres.map((item) => (
              <option key={item} value={item}>{item}</option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="coordenador-refresh"
          onClick={() => setTentativa((t) => t + 1)}
          disabled={carregando}
        >
          <RefreshCw size={15} aria-hidden="true" />
          {carregando ? 'Atualizando...' : 'Atualizar'}
        </button>
      </section>

      {carregando && (
        <p className="coordenador-load-note" role="status">Carregando avaliações...</p>
      )}

      {erro && (
        <div className="coordenador-load-error" role="alert">
          <div>
            {erro.titulo && <strong>{erro.titulo}</strong>}
            <p>{erro.descricao}</p>
          </div>
          <button
            type="button"
            className="coordenador-retry"
            onClick={() => setTentativa((t) => t + 1)}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {dados && dados.totalRespostas === 0 && (
        <p className="coordenador-empty-state" role="status">
          Nenhuma avaliação respondida neste período. Pesquisas sem resposta não entram no cálculo.
        </p>
      )}

      {dados && dados.totalRespostas > 0 && (
        <>
          <section className="satisfacao-resumo" aria-label="Resumo das avaliações">
            <div className="satisfacao-card satisfacao-media">
              <span className="satisfacao-rotulo">Nota média geral</span>
              <strong className="satisfacao-media-valor">
                {notaBR.format(dados.mediaGeral)} <small>/ 5,0</small>
              </strong>
              <Estrelas valor={dados.mediaGeral} />
              <span className="satisfacao-total">
                {inteiroBR.format(dados.totalRespostas)}{' '}
                {dados.totalRespostas === 1 ? 'avaliação respondida' : 'avaliações respondidas'}
              </span>
              <span className="satisfacao-total">{dados.semestre || 'Todos os períodos'}</span>
            </div>

            <div className="satisfacao-card">
              <span className="satisfacao-rotulo">Distribuição das notas</span>
              <div className="satisfacao-distribuicao">
                {dados.distribuicao.map((linha) => {
                  const percentual = Math.round((linha.total * 100) / dados.totalRespostas);
                  return (
                    <div className="satisfacao-linha" key={linha.nota}>
                      <span>{linha.nota} ★</span>
                      <div className="satisfacao-trilho">
                        <div className="satisfacao-preenchimento" style={{ width: `${percentual}%` }} />
                      </div>
                      <span className="satisfacao-linha-valor">{inteiroBR.format(linha.total)}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>

          <section className="satisfacao-secao" aria-label="Lista de avaliações">
            <h2>Avaliações recebidas</h2>
            <ul className="satisfacao-lista">
              {dados.avaliacoes.map((avaliacao, indice) => (
                <li className="satisfacao-item" key={`${dados.pagina}-${indice}`}>
                  <div className="satisfacao-item-topo">
                    <Estrelas valor={avaliacao.nota} />
                    <strong className="satisfacao-item-nota">
                      {avaliacao.nota} {avaliacao.nota === 1 ? 'estrela' : 'estrelas'}
                    </strong>
                    <span className="satisfacao-item-meta">{formatarData(avaliacao.dataResposta)}</span>
                    <span className="satisfacao-item-meta">Atendido por {avaliacao.aluno}</span>
                  </div>
                  {avaliacao.comentario ? (
                    <p className="satisfacao-comentario">{avaliacao.comentario}</p>
                  ) : (
                    <p className="satisfacao-comentario satisfacao-sem-comentario">Sem comentário.</p>
                  )}
                </li>
              ))}
            </ul>

            {dados.totalPaginas > 1 && (
              <nav className="satisfacao-paginacao" aria-label="Paginação das avaliações">
                <button
                  type="button"
                  onClick={() => setPagina((p) => Math.max(p - 1, 0))}
                  disabled={pagina === 0}
                >
                  Anterior
                </button>
                <span>Página {pagina + 1} de {dados.totalPaginas}</span>
                <button
                  type="button"
                  onClick={() => setPagina((p) => p + 1)}
                  disabled={pagina + 1 >= dados.totalPaginas}
                >
                  Próxima
                </button>
              </nav>
            )}

            <p className="satisfacao-nota-lgpd">
              Relatório anônimo: o nome do paciente não é exibido.
            </p>
          </section>
        </>
      )}
    </>
  );
}

export default RelatorioSatisfacao;