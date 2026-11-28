import { useCallback, useEffect, useMemo, useState } from 'react'
import { Bell, CalendarPlus, Eye, RefreshCw, Search, X } from 'lucide-react'
import {
  cancelarAgendamento,
  criarAgendamento,
  listarAgendamentos,
  listarAlunosElegiveis,
  listarDisponibilidade,
  listarFilaParaAgendamento,
  listarNotificacoes,
  simularRespostaWhatsapp,
  remarcarAgendamento,
} from '../services/agendamentoService.js'
import './AgendamentosSecretaria.css'
import { listarGradeSemanal } from '../services/VagasService.js'

const hoje = () => new Date().toLocaleDateString('en-CA')
const maisTarde = (dias) => {
  const data = new Date()
  data.setDate(data.getDate() + dias)
  return data.toLocaleDateString('en-CA')
}
const isoLocal = (data) => `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`
const semanaInicio = (valor) => {
  const data = new Date(`${valor}T12:00:00`)
  data.setDate(data.getDate() - ((data.getDay() + 6) % 7))
  return isoLocal(data)
}
const somaDias = (valor, quantidade) => {
  const data = new Date(`${valor}T12:00:00`)
  data.setDate(data.getDate() + quantidade)
  return isoLocal(data)
}
const dataBR = (data) => data ? new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR') : '—'
const turnoBR = (turno) => turno === 'MANHA' ? 'Manhã' : turno === 'TARDE' ? 'Tarde' : turno || '—'

function erroLegivel(erro) {
  if (erro?.status >= 500 || erro?.response?.status >= 500) return 'Servico de agendamentos indisponivel. Tente novamente.'
  if (erro?.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.'
  if (erro?.status === 403) return 'Somente a Secretaria pode gerenciar agendamentos.'
  if (erro?.status === 409) return erro.message || 'A vaga mudou. Atualize a disponibilidade e tente novamente.'
  return erro?.message || 'Não foi possível concluir a operação. Tente novamente.'
}

function AgendamentosSecretaria({ slotInicial, aoLimparSlot, aoAlterar }) {
  const [agendamentos, setAgendamentos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [pesquisa, setPesquisa] = useState('')
  const [filtroData, setFiltroData] = useState('')
  const [filtroAluno, setFiltroAluno] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')
  const [modalNovo, setModalNovo] = useState(true)
  const [remarcando, setRemarcando] = useState(null)
  const [notificacoes, setNotificacoes] = useState(null)
  const [comprovante, setComprovante] = useState(null)
  const [fila, setFila] = useState([])
  const [alunos, setAlunos] = useState([])
  const [, setDisponibilidade] = useState([])
  const [cadeirasDisponiveis, setCadeirasDisponiveis] = useState([])
  const [cadeirasRemarcacao, setCadeirasRemarcacao] = useState([])
  const [alternativas, setAlternativas] = useState([])
  const [enviando, setEnviando] = useState(false)
  const [erroFormulario, setErroFormulario] = useState('')
  const [form, setForm] = useState({ filaId: '', alunoTurma: '', data: '', turno: '', cadeiraId: '' })
  const [novaVaga, setNovaVaga] = useState({ data: '', turno: '', cadeiraId: '' })

  const filtros = useMemo(() => ({
    busca: pesquisa.trim(), data: filtroData, alunoId: filtroAluno, status: filtroStatus,
  }), [pesquisa, filtroData, filtroAluno, filtroStatus])

  const atualizar = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const dados = await listarAgendamentos(filtros)
      setAgendamentos(Array.isArray(dados) ? dados : [])
    } catch (falha) {
      setErro(erroLegivel(falha))
    } finally {
      setCarregando(false)
    }
  }, [filtros])

  useEffect(() => {
    let ativa = true
    queueMicrotask(() => { if (ativa) void atualizar() })
    return () => { ativa = false }
  }, [atualizar])

  useEffect(() => {
    let ativa = true
    if (!modalNovo) return undefined
    listarFilaParaAgendamento()
      .then((dados) => { if (ativa) setFila(Array.isArray(dados) ? dados : []) })
      .catch((falha) => { if (ativa) setErroFormulario(erroLegivel(falha)) })
    return () => { ativa = false }
  }, [modalNovo])

  useEffect(() => {
    if (!slotInicial) return
    queueMicrotask(() => {
      setForm((atual) => ({
        ...atual,
        data: slotInicial.data,
        turno: slotInicial.turno,
        cadeiraId: String(slotInicial.cadeiraId),
      }))
      setCadeirasDisponiveis([slotInicial])
      setErroFormulario('')
      setModalNovo(true)
    })
  }, [slotInicial])

  useEffect(() => {
    let ativa = true
    const [alunoId] = form.alunoTurma.split(':')
    if (!form.data || !form.turno) {
      queueMicrotask(() => { if (ativa) setCadeirasDisponiveis([]) })
      return undefined
    }
    const inicioSemana = semanaInicio(form.data)
    listarGradeSemanal(inicioSemana, somaDias(inicioSemana, 6)).then((grade) => {
      if (!ativa) return
      const livres = (Array.isArray(grade) ? grade : []).filter((item) =>
        item.data === form.data && item.turno === form.turno && item.situacao === 'LIVRE')
      setCadeirasDisponiveis(livres)
      setForm((atual) => ({
        ...atual,
        cadeiraId: livres.some((item) => String(item.cadeiraId) === atual.cadeiraId)
          ? atual.cadeiraId : livres.length === 1 ? String(livres[0].cadeiraId) : '',
      }))
    }).catch((falha) => { if (ativa) setErroFormulario(erroLegivel(falha)) })
    if (!form.filaId) {
      queueMicrotask(() => {
        if (ativa) {
          setAlunos([])
          setDisponibilidade([])
        }
      })
      return () => { ativa = false }
    }
    const consulta = {
      inicio: form.data, fim: form.data, filaId: form.filaId,
      ...(alunoId ? { alunoId } : {}),
    }
    Promise.all([
      listarAlunosElegiveis({ filaId: form.filaId, data: form.data, turno: form.turno }),
      listarDisponibilidade(consulta),
    ]).then(async ([opcoesAluno, opcoesVaga]) => {
      if (!ativa) return
      setAlunos(Array.isArray(opcoesAluno) ? opcoesAluno : [])
      const vagasDia = (Array.isArray(opcoesVaga) ? opcoesVaga : []).filter((vaga) => vaga.turno === form.turno)
      setDisponibilidade(vagasDia)
      if (!vagasDia.some((vaga) => vaga.vagasDisponiveis > 0)) {
        const proximas = await listarDisponibilidade({
          inicio: form.data, fim: maisTarde(30), filaId: form.filaId,
          ...(alunoId ? { alunoId } : {}),
        })
        if (ativa) setAlternativas((Array.isArray(proximas) ? proximas : []).slice(0, 6))
      } else {
        setAlternativas([])
      }
    }).catch((falha) => {
      if (ativa) {
        setAlunos([])
        setDisponibilidade([])
        setAlternativas([])
        setErroFormulario(erroLegivel(falha))
      }
    })
    return () => { ativa = false }
  }, [modalNovo, form.filaId, form.data, form.turno, form.alunoTurma])

  useEffect(() => {
    let ativa = true
    if (!remarcando || !novaVaga.data) {
      return undefined
    }
    listarDisponibilidade({
      inicio: novaVaga.data,
      fim: novaVaga.data,
      filaId: remarcando.filaEsperaId,
      alunoId: remarcando.alunoId,
      consultaIgnorada: remarcando.id,
    }).then(async (dados) => {
      if (!ativa) return
      const vagasDia = Array.isArray(dados) ? dados : []
      setDisponibilidade(vagasDia)
      const vagasUtilizaveis = vagasDia.filter((item) =>
        item.data !== remarcando.data || item.turno !== remarcando.turno,
      )
      if (!vagasUtilizaveis.some((item) => item.vagasDisponiveis > 0)) {
        const proximas = await listarDisponibilidade({
          inicio: novaVaga.data, fim: maisTarde(30),
          filaId: remarcando.filaEsperaId, alunoId: remarcando.alunoId,
          consultaIgnorada: remarcando.id,
        })
        if (ativa) setAlternativas((Array.isArray(proximas) ? proximas : [])
          .filter((item) => item.data !== remarcando.data || item.turno !== remarcando.turno)
          .slice(0, 6))
      } else {
        setAlternativas([])
      }
    }).catch((falha) => {
      if (ativa) setErroFormulario(erroLegivel(falha))
    })
    return () => { ativa = false }
  }, [remarcando, novaVaga.data])

  useEffect(() => {
    let ativa = true
    if (!remarcando || !novaVaga.data) {
      queueMicrotask(() => { if (ativa) setCadeirasRemarcacao([]) })
      return undefined
    }
    const inicio = semanaInicio(novaVaga.data)
    listarGradeSemanal(inicio, somaDias(inicio, 6)).then((grade) => {
      if (!ativa) return
      const livres = (Array.isArray(grade) ? grade : []).filter((item) =>
        item.data === novaVaga.data && item.situacao === 'LIVRE')
      setCadeirasRemarcacao(livres)
      if (!livres.some((item) => String(item.cadeiraId) === novaVaga.cadeiraId
        && item.turno === novaVaga.turno)) {
        setNovaVaga((atual) => ({ ...atual, cadeiraId: '' }))
      }
    }).catch((falha) => { if (ativa) setErroFormulario(erroLegivel(falha)) })
    return () => { ativa = false }
  }, [remarcando, novaVaga.data, novaVaga.turno, novaVaga.cadeiraId])

  const alunosFiltro = useMemo(() => {
    const unicos = new Map(agendamentos.map((item) => [item.alunoId, item.aluno]))
    return [...unicos.entries()].sort((a, b) => (a[1] || '').localeCompare(b[1] || ''))
  }, [agendamentos])

  function fecharNovo() {
    setModalNovo(true)
    setForm({ filaId: '', alunoTurma: '', data: '', turno: '', cadeiraId: '' })
    setAlunos([])
    setDisponibilidade([])
    setCadeirasDisponiveis([])
    setAlternativas([])
    setErroFormulario('')
    aoLimparSlot?.()
  }

  async function confirmarNovo(evento) {
    evento.preventDefault()
    const [alunoId, turmaId] = form.alunoTurma.split(':')
    const cadeira = cadeirasDisponiveis.find((item) => String(item.cadeiraId) === form.cadeiraId)
    if (!form.filaId || !alunoId || !turmaId || !form.data || !form.turno || !cadeira) {
      setErroFormulario('Selecione a demanda, o aluno, a data e um turno com vaga disponível.')
      return
    }
    setEnviando(true)
    setErroFormulario('')
    try {
      const resultado = await criarAgendamento({
        filaEsperaId: Number(form.filaId), alunoId: Number(alunoId), turmaId: Number(turmaId),
        cadeiraId: Number(form.cadeiraId), data: form.data, turno: form.turno,
      })
      setComprovante(resultado)
      fecharNovo()
      await atualizar()
      aoAlterar?.()
    } catch (falha) {
      setErroFormulario(erroLegivel(falha))
      if (falha?.status === 409) {
        const opcoes = await listarDisponibilidade({ inicio: form.data, fim: maisTarde(30), filaId: form.filaId })
          .catch(() => [])
        setDisponibilidade(Array.isArray(opcoes) ? opcoes.filter((item) => item.data === form.data) : [])
        setAlternativas(Array.isArray(opcoes) ? opcoes.filter((item) => item.vagasDisponiveis > 0).slice(0, 6) : [])
      }
    } finally {
      setEnviando(false)
    }
  }

  async function confirmarCancelamento(consulta) {
    if (!window.confirm(`Cancelar a consulta de ${consulta.paciente} em ${dataBR(consulta.data)} (${turnoBR(consulta.turno)})? O paciente retornará à fila.`)) return
    setErro('')
    try {
      await cancelarAgendamento(consulta.id)
      await atualizar()
      aoAlterar?.()
    } catch (falha) {
      setErro(erroLegivel(falha))
    }
  }

  async function confirmarRemarcacao(evento) {
    evento.preventDefault()
    const vaga = cadeirasRemarcacao.find((item) => item.turno === novaVaga.turno
      && String(item.cadeiraId) === novaVaga.cadeiraId)
    if (!vaga) {
      setErroFormulario('Não há vaga disponível para a data e o turno selecionados.')
      return
    }
    setEnviando(true)
    setErroFormulario('')
    try {
      const resultado = await remarcarAgendamento(remarcando.id, novaVaga)
      setComprovante(resultado)
      setRemarcando(null)
      setNovaVaga({ data: '', turno: '', cadeiraId: '' })
      await atualizar()
      aoAlterar?.()
    } catch (falha) {
      setErroFormulario(erroLegivel(falha))
      if (falha?.status === 409) {
        const opcoes = await listarDisponibilidade({
          inicio: novaVaga.data, fim: maisTarde(30),
          filaId: remarcando.filaEsperaId, alunoId: remarcando.alunoId,
          consultaIgnorada: remarcando.id,
        }).catch(() => [])
        setDisponibilidade(Array.isArray(opcoes) ? opcoes.filter((item) => item.data === novaVaga.data) : [])
        setAlternativas(Array.isArray(opcoes) ? opcoes.filter((item) => item.vagasDisponiveis > 0).slice(0, 6) : [])
      }
    } finally {
      setEnviando(false)
    }
  }

  async function abrirNotificacoes(consultaId) {
    try {
      const dados = await listarNotificacoes(consultaId)
      setNotificacoes(Array.isArray(dados) ? dados : [])
    } catch (falha) {
      setErro(erroLegivel(falha))
    }
  }

  async function simularResposta(item, mensagem) {
    try {
      await simularRespostaWhatsapp(item.telefonePaciente, mensagem)
      const dados = await listarNotificacoes(item.consultaId)
      setNotificacoes(Array.isArray(dados) ? dados : [])
      setErro('')
    } catch (falha) {
      setErro(erroLegivel(falha))
    }
  }
  const opcoesRemarcacao = [...new Set(cadeirasRemarcacao.map((item) => item.turno))]
  const filaSelecionada = fila.find((item) => String(item.filaEsperaId) === form.filaId)
  const selecaoAluno = alunos.find((item) => `${item.alunoId}:${item.turmaId}` === form.alunoTurma)

  return (
    <section className="agendamentos-secretaria" aria-label="Gestão de agendamentos">
      <header className="agendamentos-cabecalho">
        <div>
          <span className="agendamentos-etiqueta">SECRETARIA · CONSULTAS</span>
          <h2>Gerenciamento de agendamentos</h2>
        </div>
        <button className="agendamento-botao-principal" type="button" onClick={() => { setErroFormulario(''); setFila([]); setDisponibilidade([]); setAlternativas([]); setAlunos([]); setModalNovo(true) }}>
          <CalendarPlus size={17} /> Novo Agendamento
        </button>
      </header>

      {erro && <div className="agendamento-alerta erro" role="alert">{erro}</div>}

      <section className="agendamento-filtros" aria-label="Filtros de agendamentos">
        <label className="agendamento-pesquisa"><Search size={17} /><input value={pesquisa} onChange={(e) => setPesquisa(e.target.value)} placeholder="Pesquisar paciente" /></label>
        <label>Data<input type="date" value={filtroData} onChange={(e) => setFiltroData(e.target.value)} /></label>
        <label>Aluno<select value={filtroAluno} onChange={(e) => setFiltroAluno(e.target.value)}><option value="">Todos</option>{alunosFiltro.map(([id, nome]) => <option key={id} value={id}>{nome}</option>)}</select></label>
        <label>Status<select value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}><option value="">Todos</option><option value="AGENDADO">Agendado</option><option value="CONFIRMADA_24H">Confirmado</option><option value="CANCELADO_SECRETARIA">Cancelado</option><option value="REALIZADO">Realizado</option></select></label>
      </section>

      <section className="agendamento-tabela-card">
        <div className="agendamento-tabela-topo"><h2>Consultas</h2><span>{agendamentos.length} registros</span></div>
        <div className="agendamento-tabela-scroll">
          <table>
            <thead><tr><th>Paciente</th><th>Aluno responsável</th><th>Data</th><th>Turno</th><th>Procedimento</th><th>Status</th><th>Ações</th></tr></thead>
            <tbody>
              {carregando && <tr><td colSpan="7" className="agendamento-vazio">Carregando consultas...</td></tr>}
              {!carregando && !agendamentos.length && <tr><td colSpan="7" className="agendamento-vazio">Nenhuma consulta encontrada.</td></tr>}
              {!carregando && agendamentos.map((consulta) => {
                const ativa = consulta.status === 'AGENDADO' || consulta.status === 'CONFIRMADA_24H'
                return <tr key={consulta.id}>
                  <td><strong>{consulta.paciente}</strong><small>Consulta #{consulta.id}</small></td>
                  <td>{consulta.aluno}</td><td>{dataBR(consulta.data)}</td><td>{turnoBR(consulta.turno)}</td>
                  <td>{consulta.procedimento || consulta.especialidade || '—'}</td>
                  <td><span className={`agendamento-status ${ativa ? 'ativo' : consulta.status.startsWith('CANCELADO') ? 'cancelado' : 'finalizado'}`}>{consulta.status === 'AGENDADO' ? 'Agendado' : consulta.status === 'CONFIRMADA_24H' ? 'Confirmado' : consulta.status === 'CANCELADO_SECRETARIA' || consulta.status === 'CANCELADO_PACIENTE' ? 'Cancelado' : consulta.status}</span></td>
                  <td className="agendamento-acoes">
                    <button type="button" title="Ver notificações" onClick={() => abrirNotificacoes(consulta.id)}><Eye size={16} /></button>
                    {ativa && <>
                      <button type="button" title="Remarcar" onClick={() => { setErroFormulario(''); setDisponibilidade([]); setAlternativas([]); setNovaVaga({ data: '', turno: '', cadeiraId: '' }); setRemarcando(consulta) }}><RefreshCw size={15} /></button>
                      <button type="button" title="Cancelar consulta" onClick={() => confirmarCancelamento(consulta)}><X size={16} /></button>
                    </>}
                  </td>
                </tr>
              })}
            </tbody>
          </table>
        </div>
      </section>

      <section className="agenda-agendamento" aria-labelledby="agenda-agendamento-titulo">
        <header className="agenda-agendamento-cabecalho">
          <div><h3 id="agenda-agendamento-titulo">Agendamento, remarcação e cancelamento</h3><p>Somente a secretaria oficializa estas ações.</p></div>
        </header>
        <div className="agenda-agendamento-layout">
          <form className="agenda-painel-novo" id="novo-agendamento-form" onSubmit={confirmarNovo}>
            <header className="agenda-painel-topo">
              <div><h4>Novo agendamento</h4><p>Paciente triado da fila de espera</p></div>
              <span className="agenda-selo-urgencia">{filaSelecionada?.nivelUrgencia || 'Fila de espera'}</span>
            </header>
            <div className="agenda-info-paciente">
              <strong>{filaSelecionada?.paciente || 'Selecione um paciente da fila'}</strong>
              <span>{filaSelecionada ? `${filaSelecionada.procedimento || 'Procedimento'}${filaSelecionada.dente ? ` · dente ${filaSelecionada.dente}` : ''}` : 'Os dados da demanda aparecem aqui.'}</span>
              {filaSelecionada && <span className="agenda-selo-triagem">Triado · acesso liberado</span>}
            </div>
            {erroFormulario && <div className="agendamento-alerta erro" role="alert">{erroFormulario}</div>}
            <label>Paciente da fila<select required value={form.filaId} onChange={(e) => { setAlunos([]); setDisponibilidade([]); setAlternativas([]); setForm({ filaId: e.target.value, alunoTurma: '', data: '', turno: '', cadeiraId: '' }) }}><option value="">Selecione uma demanda</option>{fila.map((item) => <option key={item.filaEsperaId} value={item.filaEsperaId}>{item.paciente} · {item.procedimento} {item.dente ? `· dente ${item.dente}` : ''} · {item.statusFila === 'ALOCADO' ? 'Vinculado' : 'Aguardando'}</option>)}</select></label>
            <div className="agendamento-grade-campos">
              <label>Data<input type="date" min={hoje()} max={maisTarde(90)} required value={form.data} onChange={(e) => { setDisponibilidade([]); setAlternativas([]); setAlunos([]); setForm({ ...form, data: e.target.value, alunoTurma: '', cadeiraId: '' }) }} /></label>
              <label>Turno<select required value={form.turno} onChange={(e) => { setDisponibilidade([]); setAlternativas([]); setAlunos([]); setForm({ ...form, turno: e.target.value, alunoTurma: '', cadeiraId: '' }) }}><option value="">Selecione</option><option value="MANHA">Manhã · 08h às 12h</option><option value="TARDE">Tarde · 13h às 17h</option></select></label>
            </div>
            <div className="agendamento-grade-campos">
              <label>Cadeira<select required disabled={!cadeirasDisponiveis.length} value={form.cadeiraId} onChange={(e) => setForm({ ...form, cadeiraId: e.target.value })}><option value="">{!form.data || !form.turno ? 'Selecione data e turno primeiro' : cadeirasDisponiveis.length ? 'Selecione uma cadeira livre' : 'Nenhuma cadeira livre neste turno'}</option>{cadeirasDisponiveis.map((item) => <option key={item.cadeiraId} value={item.cadeiraId}>Cadeira {item.cadeiraNumero} · disponível</option>)}</select></label>
              <label>Aluno responsável<select required disabled={!alunos.length} value={form.alunoTurma} onChange={(e) => setForm({ ...form, alunoTurma: e.target.value })}><option value="">{!form.data || !form.turno ? 'Selecione data e turno primeiro' : alunos.length ? 'Selecione um aluno' : 'Nenhum aluno elegível disponível'}</option>{alunos.map((item) => <option key={`${item.alunoId}:${item.turmaId}`} value={`${item.alunoId}:${item.turmaId}`}>{item.aluno} · {item.turma}</option>)}</select></label>
            </div>
            {form.data && form.turno && <p className={`agendamento-disponivel ${cadeirasDisponiveis.length ? '' : 'indisponivel'}`}>{cadeirasDisponiveis.length ? `Vaga validada — ${cadeirasDisponiveis.length} cadeira(s) livre(s) neste turno.` : 'Sem cadeiras livres para essa data e turno. Tente outra opção.'}</p>}
            {!cadeirasDisponiveis.length && alternativas.length > 0 && <div className="agendamento-alternativas"><strong>Próximas opções disponíveis</strong><div>{alternativas.map((item) => <button type="button" key={`${item.data}-${item.turno}`} onClick={() => setForm({ ...form, data: item.data, turno: item.turno, alunoTurma: '', cadeiraId: '' })}>{dataBR(item.data)} · {turnoBR(item.turno)} ({item.vagasDisponiveis})</button>)}</div></div>}
            <div className="agenda-form-acoes"><button className="agendamento-botao-principal" type="submit" disabled={enviando || !form.cadeiraId || !cadeirasDisponiveis.some((item) => String(item.cadeiraId) === form.cadeiraId)}><CalendarPlus size={15} />{enviando ? 'Agendando...' : 'Agendar consulta'}</button></div>
          </form>
          <aside className="agenda-resumo">
            <h4>Resumo do agendamento</h4>
            <strong className="agenda-resumo-data">{form.data ? dataBR(form.data) : 'Selecione uma data'}{form.turno ? ` · ${turnoBR(form.turno).toLowerCase()}` : ''}</strong>
            <p>{filaSelecionada?.paciente || 'Paciente da fila'}{form.cadeiraId ? ` · cadeira ${cadeirasDisponiveis.find((item) => String(item.cadeiraId) === form.cadeiraId)?.cadeiraNumero || ''}` : ''}</p>
            <p>{filaSelecionada?.procedimento || 'Procedimento'}{selecaoAluno ? ` · ${selecaoAluno.aluno}` : ''}</p>
            <hr />
            <span className="agenda-status-pendente">Pendente de confirmação</span>
            <small>A confirmação é enviada ao paciente pelo fluxo de notificação.</small>
          </aside>
        </div>
      </section>
      {remarcando && <div className="agendamento-modal-fundo"><form className="agendamento-modal" onSubmit={confirmarRemarcacao}>
        <button type="button" className="agendamento-fechar" aria-label="Fechar" onClick={() => { setRemarcando(null); setDisponibilidade([]); setAlternativas([]) }}><X size={18} /></button>
        <span className="agendamentos-etiqueta">ALTERAR CONSULTA #{remarcando.id}</span><h2>Remarcar consulta</h2>
        <p className="agendamento-atual">{remarcando.paciente} · {dataBR(remarcando.data)} · {turnoBR(remarcando.turno)} · {remarcando.aluno}</p>
        {erroFormulario && <div className="agendamento-alerta erro" role="alert">{erroFormulario}</div>}
        <div className="agendamento-grade-campos"><label>Nova data<input type="date" min={hoje()} max={maisTarde(90)} required value={novaVaga.data} onChange={(e) => setNovaVaga({ data: e.target.value, turno: '', cadeiraId: '' })} /></label><label>Novo turno<select required disabled={!novaVaga.data} value={novaVaga.turno} onChange={(e) => setNovaVaga({ ...novaVaga, turno: e.target.value, cadeiraId: '' })}><option value="">Selecione</option>{opcoesRemarcacao.map((turno) => <option key={turno} value={turno}>{turnoBR(turno)}</option>)}</select></label></div>
        <label>Nova cadeira<select required disabled={!novaVaga.turno} value={novaVaga.cadeiraId} onChange={(e) => setNovaVaga({ ...novaVaga, cadeiraId: e.target.value })}><option value="">Selecione uma cadeira livre</option>{cadeirasRemarcacao.filter((item) => item.turno === novaVaga.turno).map((item) => <option key={item.cadeiraId} value={item.cadeiraId}>Cadeira {item.cadeiraNumero}</option>)}</select></label>
        {novaVaga.data && !opcoesRemarcacao.length && <p className="agendamento-disponivel indisponivel">Sem outros turnos disponíveis nessa data.</p>}
        {!opcoesRemarcacao.length && alternativas.length > 0 && <div className="agendamento-alternativas"><strong>Próximas opções disponíveis</strong><div>{alternativas.map((item) => <button type="button" key={`${item.data}-${item.turno}`} onClick={() => setNovaVaga({ data: item.data, turno: item.turno })}>{dataBR(item.data)} · {turnoBR(item.turno)} ({item.vagasDisponiveis})</button>)}</div></div>}
        <div className="agendamento-modal-acoes"><button className="agendamento-botao-secundario" type="button" onClick={() => { setRemarcando(null); setDisponibilidade([]); setAlternativas([]); setCadeirasRemarcacao([]) }} disabled={enviando}>Cancelar</button><button className="agendamento-botao-principal" type="submit" disabled={enviando || !novaVaga.cadeiraId}>{enviando ? 'Remarcando...' : 'Confirmar remarcação'}</button></div>
      </form></div>}

      {comprovante && <div className="agendamento-modal-fundo"><section className="agendamento-modal">
        <button type="button" className="agendamento-fechar" aria-label="Fechar" onClick={() => setComprovante(null)}><X size={18} /></button>
        <span className="agendamentos-etiqueta">OPERAÇÃO CONCLUÍDA</span><h2>Comprovante da consulta</h2>
        <dl className="agendamento-comprovante"><div><dt>Paciente</dt><dd>{comprovante.paciente}</dd></div><div><dt>Aluno responsável</dt><dd>{comprovante.aluno}</dd></div><div><dt>Data e turno</dt><dd>{dataBR(comprovante.data)} · {turnoBR(comprovante.turno)}</dd></div><div><dt>Procedimento</dt><dd>{comprovante.procedimento || comprovante.especialidade || '—'}</dd></div><div><dt>Status</dt><dd>{comprovante.status === 'AGENDADO' ? 'Agendado' : 'Cancelado'}</dd></div></dl>
        <button className="agendamento-botao-secundario" type="button" onClick={() => abrirNotificacoes(comprovante.id)}><Bell size={15} /> Ver mensagem simulada</button>
      </section></div>}

      {notificacoes && <div className="agendamento-modal-fundo"><section className="agendamento-modal">
        <button type="button" className="agendamento-fechar" aria-label="Fechar" onClick={() => setNotificacoes(null)}><X size={18} /></button>
        <span className="agendamentos-etiqueta">WHATSAPP · SIMULAÇÃO</span><h2>Notificações geradas</h2>
        {!notificacoes.length && <p>Nenhuma notificação foi registrada para esta consulta.</p>}
        {notificacoes.map((item) => <article className="agendamento-mensagem" key={item.id}>
          <header><strong>{item.tipo.replaceAll('_', ' ')}</strong><span>{item.status === 'PENDENTE' ? 'Pendente de envio manual' : item.status}</span></header>
          <p>{item.conteudo}</p>
          <small>{new Date(item.criadaEm).toLocaleString('pt-BR')}</small>
          <div className="agendamento-whatsapp-acoes">
            <a href={item.linkWhatsapp || undefined} target="_blank" rel="noreferrer" aria-disabled={!item.linkWhatsapp}>Abrir no WhatsApp</a>
            <button type="button" disabled={!item.telefonePaciente} onClick={() => simularResposta(item, '1')}>Simular 1 · Confirmar</button>
            <button type="button" disabled={!item.telefonePaciente} onClick={() => simularResposta(item, '2')}>Simular 2 · Cancelar</button>
          </div>
          <small className="agendamento-simulacao-nota">Simulações de teste; não são mensagens reais recebidas do WhatsApp.</small>
        </article>)}
        <button className="agendamento-botao-secundario" type="button" onClick={() => setNotificacoes(null)}>Fechar</button>
      </section></div>}
    </section>
  )
}

export default AgendamentosSecretaria
