import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight, Clock3, Lock, RefreshCw, Trash2, Unlock } from 'lucide-react'
import {
  bloquearTurno,
  desbloquearVaga,
  gerarTurnos,
  listarGradeSemanal,
  removerVaga,
} from '../services/VagasService.js'

const isoLocal = (date) => {
  const ano = date.getFullYear()
  const mes = String(date.getMonth() + 1).padStart(2, '0')
  const dia = String(date.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}
const parseDate = (value) => new Date(`${value}T12:00:00`)
const addDays = (value, count) => {
  const date = parseDate(value)
  date.setDate(date.getDate() + count)
  return isoLocal(date)
}
const startOfWeek = (value) => {
  const date = parseDate(value)
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7))
  return isoLocal(date)
}
const dateLabel = (value, options = { day: '2-digit', month: '2-digit' }) =>
  parseDate(value).toLocaleDateString('pt-BR', options)
const weekDay = (value) => parseDate(value).toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')
const slotLabel = (turno) => turno === 'MANHA' ? 'Manhã · 08h às 12h' : 'Tarde · 13h às 17h'
const stateLabel = (situacao) => ({ LIVRE: 'Vaga livre', OCUPADA: 'Agendada', BLOQUEADA: 'Bloqueada' })[situacao] || situacao

function erroLegivel(erro) {
  if (erro?.status >= 500 || erro?.response?.status >= 500) return 'Nao foi possivel carregar a agenda agora. Tente novamente.'
  if (erro?.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.'
  if (erro?.status === 403) return 'Somente a Secretaria pode gerenciar a disponibilidade.'
  return erro?.message || 'Não foi possível carregar a disponibilidade.'
}

function TurnosDisponiveisSecretaria({ aoAgendar }) {
  const hoje = isoLocal(new Date())
  const [inicioSemana, setInicioSemana] = useState(() => startOfWeek(hoje))
  const [diaSelecionado, setDiaSelecionado] = useState(() => startOfWeek(hoje))
  const [turnos, setTurnos] = useState([])
  const [filtroEstado, setFiltroEstado] = useState('TODOS')
  const [filtroDisciplina, setFiltroDisciplina] = useState('TODAS')
  const [periodo, setPeriodo] = useState(() => ({ inicio: startOfWeek(hoje), fim: addDays(startOfWeek(hoje), 6) }))
  const [carregando, setCarregando] = useState(true)
  const [gerando, setGerando] = useState(false)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [atualizacao, setAtualizacao] = useState(0)
  const [bloqueando, setBloqueando] = useState(null)
  const [motivo, setMotivo] = useState('')
  const [operacaoId, setOperacaoId] = useState('')

  const dias = useMemo(() => Array.from({ length: 7 }, (_, index) => addDays(inicioSemana, index)), [inicioSemana])
  const disciplinas = useMemo(() => [...new Set(turnos.map((item) => item.disciplina).filter(Boolean))].sort(), [turnos])
  const turnosDoDia = useMemo(() => turnos
    .filter((item) => item.data === diaSelecionado)
    .filter((item) => filtroEstado === 'TODOS' || item.situacao === filtroEstado)
    .filter((item) => filtroDisciplina === 'TODAS' || item.disciplina === filtroDisciplina)
    .sort((a, b) => a.turno === b.turno ? a.cadeiraNumero.localeCompare(b.cadeiraNumero) : a.turno === 'MANHA' ? -1 : 1),
  [turnos, diaSelecionado, filtroEstado, filtroDisciplina])

  useEffect(() => {
    let ativa = true
    setCarregando(true)
    listarGradeSemanal(inicioSemana, addDays(inicioSemana, 6))
      .then((dados) => { if (ativa) { setTurnos(Array.isArray(dados) ? dados : []); setErro('') } })
      .catch((falha) => { if (ativa) setErro(erroLegivel(falha)) })
      .finally(() => { if (ativa) setCarregando(false) })
    return () => { ativa = false }
  }, [inicioSemana, atualizacao])

  function mudarSemana(delta) {
    const proxima = addDays(inicioSemana, delta * 7)
    setInicioSemana(proxima)
    setDiaSelecionado(proxima)
    setPeriodo({ inicio: proxima, fim: addDays(proxima, 6) })
  }

  async function gerarPeriodo(event) {
    event.preventDefault()
    setErro('')
    setAviso('')
    setGerando(true)
    try {
      const resultado = await gerarTurnos(periodo)
      setAviso(`${resultado.criadas} turnos criados; ${resultado.jaExistentes} já estavam cadastrados.`)
      setAtualizacao((valor) => valor + 1)
    } catch (falha) {
      setErro(erroLegivel(falha))
    } finally {
      setGerando(false)
    }
  }

  async function confirmarBloqueio(event) {
    event.preventDefault()
    if (!bloqueando) return
    setOperacaoId(`${bloqueando.cadeiraId}-${bloqueando.data}-${bloqueando.turno}`)
    setErro('')
    try {
      await bloquearTurno({
        cadeiraId: bloqueando.cadeiraId,
        data: bloqueando.data,
        turno: bloqueando.turno,
        motivo,
      })
      setAviso('Turno bloqueado.')
      setBloqueando(null)
      setMotivo('')
      setAtualizacao((valor) => valor + 1)
    } catch (falha) {
      setErro(erroLegivel(falha))
    } finally {
      setOperacaoId('')
    }
  }

  async function desbloquear(item) {
    setOperacaoId(`${item.cadeiraId}-${item.data}-${item.turno}`)
    setErro('')
    try {
      await desbloquearVaga(item.vagaId)
      setAviso('Turno desbloqueado.')
      setAtualizacao((valor) => valor + 1)
    } catch (falha) {
      setErro(erroLegivel(falha))
    } finally {
      setOperacaoId('')
    }
  }

  async function excluir(item) {
    if (!window.confirm(`Excluir a vaga da cadeira ${item.cadeiraNumero} em ${dateLabel(item.data)}?`)) return
    setOperacaoId(`${item.cadeiraId}-${item.data}-${item.turno}`)
    setErro('')
    try {
      await removerVaga(item.vagaId)
      setAviso('Turno removido. O espaço continua livre na grade.')
      setAtualizacao((valor) => valor + 1)
    } catch (falha) {
      setErro(erroLegivel(falha))
    } finally {
      setOperacaoId('')
    }
  }

  const tituloSemana = `${dateLabel(inicioSemana)} – ${dateLabel(addDays(inicioSemana, 6), { day: '2-digit', month: '2-digit', year: 'numeric' })}`

  return (
    <section className="turnos-secretaria" aria-labelledby="turnos-titulo">
      <header className="turnos-cabecalho">
        <div>
          <p className="turnos-eyebrow">SECRETARIA · AGENDA</p>
          <h2 id="turnos-titulo">Agenda semanal</h2>
          <p>Consultas, turnos e cadeiras por dia.</p>
        </div>
        <details className="turnos-geracao-menu">
          <summary><CalendarDays size={15} /> Gerar turnos</summary>
          <form className="turnos-geracao" onSubmit={gerarPeriodo}>
            <label>Início<input type="date" required value={periodo.inicio} onChange={(event) => setPeriodo({ ...periodo, inicio: event.target.value })} /></label>
            <label>Fim<input type="date" required min={periodo.inicio} value={periodo.fim} onChange={(event) => setPeriodo({ ...periodo, fim: event.target.value })} /></label>
            <button className="turnos-botao-principal" type="submit" disabled={gerando || !periodo.inicio || !periodo.fim}>
              {gerando ? 'Gerando…' : 'Confirmar geração'}
            </button>
          </form>
        </details>
      </header>

      {erro && <p className="turnos-mensagem erro" role="alert">{erro}</p>}
      {aviso && <p className="turnos-mensagem sucesso" role="status">{aviso}</p>}

      <section className="turnos-grade-painel" aria-label="Grade semanal de turnos">
        <div className="turnos-grade-topo">
          <div className="turnos-semana-navegacao">
            <button type="button" aria-label="Semana anterior" onClick={() => mudarSemana(-1)}><ChevronLeft size={18} /></button>
            <strong>{tituloSemana}</strong>
            <button type="button" aria-label="Próxima semana" onClick={() => mudarSemana(1)}><ChevronRight size={18} /></button>
          </div>
          <div className="turnos-filtros">
            <label className="turnos-filtro-estado">Disciplina
              <select value={filtroDisciplina} onChange={(event) => setFiltroDisciplina(event.target.value)}>
                <option value="TODAS">Todas as disciplinas</option>
                {disciplinas.map((disciplina) => <option key={disciplina} value={disciplina}>{disciplina}</option>)}
              </select>
            </label>
            <label className="turnos-filtro-estado">Disponibilidade
              <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
                <option value="TODOS">Todos os estados</option>
                <option value="LIVRE">Vagas livres</option>
                <option value="OCUPADA">Ocupadas</option>
                <option value="BLOQUEADA">Bloqueadas</option>
              </select>
            </label>
          </div>
        </div>

        <div className="turnos-dias" role="tablist" aria-label="Dias da semana">
          {dias.map((dia) => (
            <button key={dia} type="button" role="tab" aria-selected={diaSelecionado === dia}
              className={diaSelecionado === dia ? 'selecionado' : ''} onClick={() => setDiaSelecionado(dia)}>
              <span>{weekDay(dia)}</span><strong>{dateLabel(dia, { day: '2-digit' })}</strong>
            </button>
          ))}
        </div>

        <div className="turnos-dia-titulo">
          <div><h3>{parseDate(diaSelecionado).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })}</h3>
            <p>{turnosDoDia.length} cadeiras/turnos nesta visualização</p></div>
          <button className="turnos-botao-secundario" type="button" onClick={() => setAtualizacao((valor) => valor + 1)} disabled={carregando}>
            <RefreshCw size={15} /> Atualizar
          </button>
        </div>

        {carregando ? <p className="turnos-vazio">Carregando grade…</p> : !turnosDoDia.length ? (
          <p className="turnos-vazio">{turnos.length === 0
            ? 'Nenhuma cadeira ativa cadastrada. Ative as cadeiras para montar a agenda.'
            : 'Não há turnos para este dia e filtro.'}</p>
        ) : ['MANHA', 'TARDE'].map((turno) => {
          const itens = turnosDoDia.filter((item) => item.turno === turno)
          if (!itens.length) return null
          return <section className="turnos-faixa" key={turno} aria-label={slotLabel(turno)}>
            <h4><Clock3 size={16} />{slotLabel(turno)}</h4>
            <div className="turnos-cartoes">
              {itens.map((item) => {
                const idOperacao = `${item.cadeiraId}-${item.data}-${item.turno}`
                return <article className={`turno-card estado-${item.situacao.toLowerCase()}`} key={idOperacao}>
                  <span className="turno-card-cadeira">Cadeira {item.cadeiraNumero} · {turno === 'MANHA' ? '08h' : '13h'}</span>
                  <strong>{item.situacao === 'OCUPADA' ? item.paciente || 'Consulta agendada' : stateLabel(item.situacao)}</strong>
                  <span className="turno-card-disciplina">{item.situacao === 'OCUPADA' ? `${item.disciplina || 'Atendimento'}${item.aluno ? ` · ${item.aluno}` : ''}` : item.motivoBloqueio || (item.situacao === 'LIVRE' ? 'Disponível para agendamento' : 'Indisponível')}</span>
                  {item.situacao === 'BLOQUEADA' && item.vagaId && <button type="button" className="turno-card-acao" disabled={operacaoId === idOperacao} onClick={() => desbloquear(item)}><Unlock size={14} /> Desbloquear</button>}
                  {item.situacao === 'LIVRE' && <div className="turno-card-acoes">
                    {aoAgendar && <button type="button" className="turno-card-acao" onClick={() => aoAgendar(item)}><CalendarDays size={13} /> Agendar</button>}
                    {item.vagaId && <button type="button" className="turno-card-acao excluir" disabled={operacaoId === idOperacao} onClick={() => excluir(item)}><Trash2 size={13} /> Excluir</button>}
                    <button type="button" className="turno-card-acao" onClick={() => { setBloqueando(item); setMotivo('') }}><Lock size={13} /> Bloquear</button>
                  </div>}
                  {bloqueando && bloqueando.cadeiraId === item.cadeiraId && bloqueando.data === item.data && bloqueando.turno === item.turno && (
                    <form className="turno-card-bloqueio" onSubmit={confirmarBloqueio}>
                      <label>Motivo<input autoFocus maxLength={200} required value={motivo} onChange={(event) => setMotivo(event.target.value)} /></label>
                      <div><button type="button" onClick={() => setBloqueando(null)}>Cancelar</button><button type="submit" disabled={Boolean(operacaoId)}>Salvar bloqueio</button></div>
                    </form>
                  )}
                </article>
              })}
            </div>
          </section>
        })}

        <div className="turnos-legenda" aria-label="Legenda de estados">
          <span className="legenda-livre">Vaga livre</span><span className="legenda-ocupada">Agendada</span><span className="legenda-bloqueada">Bloqueada</span>
        </div>
      </section>
    </section>
  )
}

export default TurnosDisponiveisSecretaria
