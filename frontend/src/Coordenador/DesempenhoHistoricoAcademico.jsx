import { useEffect, useRef, useState } from 'react';
import { buscarDesempenhoAcademico } from '../services/desempenhoAcademicoService.js';
import './DesempenhoHistoricoAcademico.css';

const numeroBR = new Intl.NumberFormat('pt-BR');

function mensagemDeErro(erro) {
  switch (erro.status) {
    case 400:
      return 'Confira a modalidade, a matrícula ou o código da turma e o período.';
    case 401:
      return 'Sua sessão expirou. Entre novamente para continuar consultando.';
    case 403:
      return 'Você não tem permissão para consultar o desempenho acadêmico.';
    case 404:
      return 'Nenhum registro foi localizado.';
    default:
      return 'Não foi possível consultar os dados. Verifique a conexão e tente novamente.';
  }
}

function progressoFormatado(valor) {
  return `${numeroBR.format(valor)}%`;
}

function TabelaHistorico({ resposta }) {
  return (
    <section className="desempenho-section" aria-labelledby="desempenho-historico-titulo">
      <div className="desempenho-section-heading">
        <h2 id="desempenho-historico-titulo">Histórico consolidado por disciplina</h2>
        <span>{resposta.semestre}</span>
      </div>
      {resposta.historico.length === 0 ? (
        <p className="desempenho-empty" role="status">Não há metas acadêmicas ativas neste período.</p>
      ) : (
        <div className="desempenho-table-scroll">
          <table className="desempenho-table">
            <thead>
              <tr>
                <th scope="col">Disciplina / tratamento</th>
                <th scope="col">Exigidos</th>
                <th scope="col">Concluídos</th>
                <th scope="col">Pendentes</th>
                <th scope="col">Progresso</th>
              </tr>
            </thead>
            <tbody>
              {resposta.historico.map((linha, indice) => (
                <tr key={`${linha.disciplina}-${linha.procedimento}-${indice}`}>
                  <th scope="row">
                    <span>{linha.disciplina}</span>
                    <small>{linha.procedimento}</small>
                  </th>
                  <td>{numeroBR.format(linha.exigidos)}</td>
                  <td>{numeroBR.format(linha.concluidos)}</td>
                  <td>{numeroBR.format(linha.pendentes)}</td>
                  <td>{progressoFormatado(linha.progressoPercentual)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th scope="row">Total do período</th>
                <td>{numeroBR.format(resposta.resumo.exigidos)}</td>
                <td>{numeroBR.format(resposta.resumo.concluidos)}</td>
                <td>{numeroBR.format(resposta.resumo.pendentes)}</td>
                <td>{progressoFormatado(resposta.resumo.progressoPercentual)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </section>
  );
}

function TabelaTurma({ resposta }) {
  return (
    <section className="desempenho-section" aria-labelledby="desempenho-turma-titulo">
      <div className="desempenho-section-heading">
        <h2 id="desempenho-turma-titulo">Comparativo compacto – turma {resposta.codigoTurma}</h2>
        <span>{resposta.semestre}</span>
      </div>
      {resposta.alunos.length === 0 ? (
        <p className="desempenho-empty" role="status">Nenhum aluno está matriculado nas turmas localizadas.</p>
      ) : (
        <div className="desempenho-table-scroll">
          <table className="desempenho-table desempenho-table-compact">
            <thead>
              <tr>
                <th scope="col">Aluno / matrícula</th>
                <th scope="col">Exigidos</th>
                <th scope="col">Concluídos</th>
                <th scope="col">Pendentes</th>
                <th scope="col">Progresso</th>
              </tr>
            </thead>
            <tbody>
              {resposta.alunos.map((aluno) => (
                <tr key={aluno.matriculaAluno}>
                  <th scope="row">
                    <span>{aluno.nomeAluno}</span>
                    <small>{aluno.matriculaAluno}</small>
                  </th>
                  <td>{numeroBR.format(aluno.exigidos)}</td>
                  <td>{numeroBR.format(aluno.concluidos)}</td>
                  <td>{numeroBR.format(aluno.pendentes)}</td>
                  <td>{progressoFormatado(aluno.progressoPercentual)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th scope="row">Total da turma</th>
                <td>{numeroBR.format(resposta.resumo.exigidos)}</td>
                <td>{numeroBR.format(resposta.resumo.concluidos)}</td>
                <td>{numeroBR.format(resposta.resumo.pendentes)}</td>
                <td>{progressoFormatado(resposta.resumo.progressoPercentual)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </section>
  );
}

export default function DesempenhoHistoricoAcademico({
  semestres,
  carregandoSemestres,
  erroSemestres,
  onRetrySemestres,
}) {
  const [modalidade, setModalidade] = useState('ALUNO');
  const [identificador, setIdentificador] = useState('');
  const [semestre, setSemestre] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState('');
  const [resposta, setResposta] = useState(null);
  const requisicaoAtual = useRef(null);
  const sequenciaRequisicao = useRef(0);
  const periodoSelecionado = semestres.includes(semestre) ? semestre : semestres[0] || '';

  useEffect(() => () => requisicaoAtual.current?.abort(), []);

  function invalidarConsulta() {
    requisicaoAtual.current?.abort();
    requisicaoAtual.current = null;
    sequenciaRequisicao.current += 1;
    setCarregando(false);
    setErro('');
    setResposta(null);
  }

  async function consultar(evento) {
    evento.preventDefault();
    const controlador = new AbortController();
    const sequencia = ++sequenciaRequisicao.current;
    requisicaoAtual.current?.abort();
    requisicaoAtual.current = controlador;
    setCarregando(true);
    setErro('');
    setResposta(null);

    try {
      const resultado = await buscarDesempenhoAcademico({
        modalidade,
        identificador: identificador.trim(),
        semestre: periodoSelecionado,
      }, { signal: controlador.signal });
      if (controlador.signal.aborted || sequencia !== sequenciaRequisicao.current) return;
      setResposta(resultado);
    } catch (falha) {
      if (controlador.signal.aborted || sequencia !== sequenciaRequisicao.current) return;
      setErro(mensagemDeErro(falha));
    } finally {
      if (sequencia === sequenciaRequisicao.current) {
        requisicaoAtual.current = null;
        setCarregando(false);
      }
    }
  }

  function alterarModalidade(evento) {
    invalidarConsulta();
    setIdentificador('');
    setModalidade(evento.target.value);
  }

  function alterarIdentificador(evento) {
    invalidarConsulta();
    setIdentificador(evento.target.value);
  }

  function alterarSemestre(evento) {
    invalidarConsulta();
    setSemestre(evento.target.value);
  }

  const identificadorValido = identificador.trim().length > 0;
  const periodoIndisponivel = carregandoSemestres || Boolean(erroSemestres) || semestres.length === 0;
  const rotuloIdentificador = modalidade === 'ALUNO' ? 'Matrícula do aluno' : 'Código da turma';

  return (
    <>
      <div className="coordenador-page-heading">
        <div>
          <p className="coordenador-eyebrow">COORDENAÇÃO</p>
          <h1>Desempenho e histórico acadêmico</h1>
          <p className="coordenador-welcome">Consulte o progresso acadêmico por aluno ou turma.</p>
        </div>
      </div>

      <form className="desempenho-filters" aria-label="Filtros de desempenho acadêmico" onSubmit={consultar}>
        <label htmlFor="desempenho-modalidade">
          <span>Visualização</span>
          <select id="desempenho-modalidade" value={modalidade} onChange={alterarModalidade}>
            <option value="ALUNO">Aluno</option>
            <option value="TURMA">Turma</option>
          </select>
        </label>
        <label htmlFor="desempenho-identificador">
          <span>{rotuloIdentificador}</span>
          <input
            id="desempenho-identificador"
            type="text"
            maxLength={50}
            autoComplete="off"
            value={identificador}
            onChange={alterarIdentificador}
            placeholder={modalidade === 'ALUNO' ? 'Informe a matrícula' : 'Informe o código da turma'}
            required
          />
        </label>
        <label htmlFor="desempenho-semestre">
          <span>Período</span>
          <select
            id="desempenho-semestre"
            value={periodoSelecionado}
            disabled={periodoIndisponivel}
            onChange={alterarSemestre}
            required
          >
            <option value="" disabled>Selecione</option>
            {semestres.map((periodo) => (
              <option key={periodo} value={periodo}>{periodo}</option>
            ))}
          </select>
        </label>
        <button type="submit" className="desempenho-submit" disabled={!identificadorValido || !periodoSelecionado || periodoIndisponivel}>
          Consultar
        </button>
      </form>

      {carregandoSemestres && <p className="desempenho-state" role="status">Carregando períodos letivos...</p>}
      {erroSemestres && !carregandoSemestres && (
        <div className="desempenho-error" role="alert">
          <span>{erroSemestres.descricao}</span>
          <button type="button" onClick={onRetrySemestres}>Tentar novamente</button>
        </div>
      )}
      {!carregandoSemestres && !erroSemestres && semestres.length === 0 && (
        <p className="desempenho-state" role="status">Nenhum período letivo cadastrado.</p>
      )}

      {carregando && <p className="desempenho-state" role="status" aria-live="polite">Consultando desempenho acadêmico...</p>}
      {erro && !carregando && <p className="desempenho-error" role="alert">{erro}</p>}

      {resposta && (
        <div className="desempenho-results" aria-live="polite">
          {resposta.resumo.exigidos === 0 && (
            <p className="desempenho-empty" role="status">Não há metas acadêmicas ativas para esta consulta.</p>
          )}

          <section className="desempenho-summary" aria-label="Resumo de desempenho">
            <article className="desempenho-card">
              <span>Exigidos</span>
              <strong>{numeroBR.format(resposta.resumo.exigidos)}</strong>
              <small>Meta do período</small>
            </article>
            <article className="desempenho-card">
              <span>Concluídos</span>
              <strong>{numeroBR.format(resposta.resumo.concluidos)}</strong>
              <small>{progressoFormatado(resposta.resumo.progressoPercentual)} do exigido</small>
            </article>
            <article className="desempenho-card">
              <span>Pendentes</span>
              <strong>{numeroBR.format(resposta.resumo.pendentes)}</strong>
              <small>Restantes no período</small>
            </article>
          </section>

          <TabelaHistorico resposta={resposta} />
          {resposta.modalidade === 'TURMA' && <TabelaTurma resposta={resposta} />}

          <p className="desempenho-footnote">
            Consolidado acadêmico do período, sem conteúdo de prontuários ou informações de pacientes.
          </p>
        </div>
      )}
    </>
  );
}
