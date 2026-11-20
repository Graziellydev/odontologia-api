import { useEffect, useMemo, useState } from 'react'
import { buscarPainelDeMetas } from '../services/metaService.js'
import ProntuarioDigital from './ProntuarioDigital.jsx'
import './PainelAluno.css'

// Lista os últimos semestres letivos no formato do banco (ex.: 2026.2)
function gerarSemestres(quantidade = 4) {
  const hoje = new Date()
  let ano = hoje.getFullYear()
  let semestre = hoje.getMonth() < 6 ? 1 : 2
  const lista = []
  for (let i = 0; i < quantidade; i++) {
    lista.push(`${ano}.${semestre}`)
    if (semestre === 1) {
      ano -= 1
      semestre = 2
    } else {
      semestre = 1
    }
  }
  return lista
}

function percentual(concluido, exigido) {
  return exigido === 0 ? 0 : Math.floor((concluido * 100) / exigido)
}

function mensagemDeErro(erro) {
  if (erro.status === 401 || erro.status === 403) {
    return 'Sua sessão expirou ou você não tem acesso a este painel. Faça login novamente.'
  }
  if (erro.status === 404) return 'Aluno não encontrado.'
  if (erro.status) return 'A API respondeu com um erro. Tente novamente.'
  return erro.message
}

function PainelAluno({ usuario }) {
  const semestres = useMemo(() => gerarSemestres(), [])
  const [semestre, setSemestre] = useState(semestres[0])
  const [disciplina, setDisciplina] = useState('')
  const [tentativa, setTentativa] = useState(0)
  const [resultado, setResultado] = useState({ chave: null, dados: null, erro: '' })

  // Se o resultado guardado não for desta chave, a consulta ainda está carregando
  const chave = `${semestre}|${tentativa}`

  useEffect(() => {
    let cancelado = false
    buscarPainelDeMetas(semestre)
      .then((dados) => {
        if (!cancelado) setResultado({ chave, dados, erro: '' })
      })
      .catch((erro) => {
        if (!cancelado) setResultado({ chave, dados: null, erro: mensagemDeErro(erro) })
      })
    return () => {
      cancelado = true
    }
  }, [semestre, chave])

  const carregando = resultado.chave !== chave
  const dados = carregando ? null : resultado.dados
  const erro = carregando ? '' : resultado.erro

  // Tudo o que a tela mostra, já filtrado pela disciplina escolhida
  const visao = useMemo(() => {
    if (!dados) return null

    const disciplinas = [...dados.progressoPorDisciplina].sort((a, b) =>
      a.disciplina.localeCompare(b.disciplina)
    )
    const disciplinaAtiva = disciplinas.some((d) => d.disciplina === disciplina) ? disciplina : ''
    const selecionadas = disciplinaAtiva
      ? disciplinas.filter((d) => d.disciplina === disciplinaAtiva)
      : disciplinas

    const procedimentos = dados.procedimentos
      .filter((p) => !disciplinaAtiva || p.disciplina === disciplinaAtiva)
      .sort(
        (a, b) =>
          a.disciplina.localeCompare(b.disciplina) || a.procedimento.localeCompare(b.procedimento)
      )

    const exigido = selecionadas.reduce((soma, d) => soma + d.totalExigido, 0)
    const concluido = selecionadas.reduce((soma, d) => soma + d.totalConcluido, 0)
    const saldo = selecionadas.reduce((soma, d) => soma + d.saldoPendente, 0)

    return {
      disciplinas,
      disciplinaAtiva,
      selecionadas,
      procedimentos,
      exigido,
      concluido,
      saldo,
      percentual: percentual(concluido, exigido),
    }
  }, [dados, disciplina])

  const primeiroNome = (usuario?.nome || '').split(' ')[0]

  return (
    <div className="painel">
      <h1 className="painel-titulo">Olá, {primeiroNome}. Bom turno!</h1>
      <p className="painel-sub">Acompanhe suas metas e prepare o próximo atendimento.</p>

      <ProntuarioDigital usuario={usuario} />

      <div className="painel-filtros">
        <select
          value={semestre}
          onChange={(e) => setSemestre(e.target.value)}
          aria-label="Semestre"
        >
          {semestres.map((s) => (
            <option key={s} value={s}>
              Semestre {s}
            </option>
          ))}
        </select>

        <select
          value={visao ? visao.disciplinaAtiva : ''}
          onChange={(e) => setDisciplina(e.target.value)}
          disabled={!visao || visao.disciplinas.length === 0}
          aria-label="Disciplina"
        >
          <option value="">Todas as disciplinas</option>
          {visao &&
            visao.disciplinas.map((d) => (
              <option key={d.disciplinaId} value={d.disciplina}>
                {d.disciplina}
              </option>
            ))}
        </select>
      </div>

      {carregando && <p className="painel-aviso">Carregando suas metas...</p>}

      {erro && (
        <div className="painel-erro">
          <span>⚠️ {erro}</span>
          <button onClick={() => setTentativa((t) => t + 1)}>Tentar novamente</button>
        </div>
      )}

      {visao && visao.exigido === 0 && (
        <div className="painel-vazio">Nenhuma meta cadastrada para este período.</div>
      )}

      {visao && visao.exigido > 0 && (
        <>
          {visao.saldo === 0 && (
            <div className="painel-sucesso">
              Meta cumprida! Você concluiu todos os procedimentos exigidos.
            </div>
          )}

          <section className="painel-cards">
            <div className="painel-card">
              <span className="painel-card-rotulo">Procedimentos exigidos</span>
              <strong className="painel-card-numero">{visao.exigido}</strong>
              <span className="painel-card-nota">Meta acadêmica do semestre</span>
            </div>

            <div className="painel-card destaque">
              <span className="painel-card-rotulo">Concluídos</span>
              <strong className="painel-card-numero laranja">{visao.concluido}</strong>
              <span className="painel-card-nota">{visao.percentual}% da meta cumprida</span>
            </div>

            <div className="painel-card">
              <span className="painel-card-rotulo">Saldo pendente</span>
              <strong className="painel-card-numero">{visao.saldo}</strong>
              <span className="painel-card-nota">
                {visao.exigido} exigidos − {visao.concluido} concluídos
              </span>
            </div>
          </section>

          <section className="painel-grade">
            <div className="painel-bloco">
              <h2>Seu progresso por disciplina</h2>
              <p className="painel-bloco-sub">Metas individuais • {semestre}</p>

              {visao.selecionadas.map((d) => (
                <div className="painel-progresso" key={d.disciplinaId}>
                  <div className="painel-progresso-linha">
                    <span>{d.disciplina}</span>
                    <small>
                      {d.totalConcluido} de {d.totalExigido} • {d.saldoPendente}{' '}
                      {d.saldoPendente === 1 ? 'pendente' : 'pendentes'}
                    </small>
                  </div>
                  <div className="painel-barra">
                    <div style={{ width: `${percentual(d.totalConcluido, d.totalExigido)}%` }} />
                  </div>
                </div>
              ))}

              <p className="painel-resumo">
                Você concluiu {visao.concluido} de {visao.exigido} procedimentos. Continue
                acompanhando o saldo de cada disciplina.
              </p>
            </div>

            <div className="painel-bloco">
              <h2>Procedimentos da meta</h2>
              <p className="painel-bloco-sub">Concluídos / a realizar</p>

              <table className="painel-tabela">
                <thead>
                  <tr>
                    <th>Procedimento</th>
                    <th>Concluído</th>
                    <th>A realizar</th>
                  </tr>
                </thead>
                <tbody>
                  {visao.procedimentos.map((p) => (
                    <tr key={`${p.disciplina}-${p.procedimento}`}>
                      <td>
                        {p.procedimento}
                        <small className="painel-disciplina">{p.disciplina}</small>
                      </td>
                      <td>{p.quantidadeConcluida}</td>
                      <td className={p.saldoPendente > 0 ? 'laranja' : ''}>{p.saldoPendente}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <p className="painel-rodape-tabela">
                {visao.procedimentos.length}{' '}
                {visao.procedimentos.length === 1 ? 'registro exibido' : 'registros exibidos'}
              </p>
              <span className="painel-selo">{visao.percentual}% da meta do semestre</span>
            </div>
          </section>
        </>
      )}
    </div>
  )
}

export default PainelAluno
