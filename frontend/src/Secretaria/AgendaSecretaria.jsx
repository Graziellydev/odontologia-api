import { useEffect, useState } from 'react'
import { listarFaltas, reagendarFalta, registrarFalta } from '../services/FaltasService.js'
import PainelRegistroFalta from './PainelRegistroFalta.jsx'
import PainelReagendamento from './PainelReagendamento.jsx'
import TabelaHistoricoAusencias from './TabelaHistoricoAusencias.jsx'
import TurnosDisponiveisSecretaria from './TurnosDisponiveisSecretaria.jsx'
import AgendamentosSecretaria from './AgendamentosSecretaria.jsx'
import './AgendaSecretaria.css'

function mensagemDeErro(erro) {
  if (erro?.status >= 500 || erro?.response?.status >= 500) return 'Servico da agenda indisponivel. Tente novamente em instantes.'
  const detalhe = erro?.response?.data?.detail || erro?.response?.data?.message || erro?.response?.data?.error
  if (erro?.response?.status === 401) return 'Sua sessão expirou. Faça login novamente.'
  if (detalhe) return detalhe
  if (erro?.response?.status) return `Não foi possível concluir a operação (HTTP ${erro.response.status}).`
  return erro?.message || 'Não foi possível acessar o serviço. Tente novamente.'
}

function AgendaSecretaria() {
  const [faltas, setFaltas] = useState([])
  const [consultaSelecionada, setConsultaSelecionada] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [enviandoFalta, setEnviandoFalta] = useState(false)
  const [enviandoReagendamento, setEnviandoReagendamento] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [atualizacao, setAtualizacao] = useState(0)
  const [slotInicial, setSlotInicial] = useState(null)
  const [atualizacaoAgenda, setAtualizacaoAgenda] = useState(0)

  useEffect(() => {
    let ativa = true
    listarFaltas()
      .then((dados) => {
        if (ativa) {
          setFaltas(dados)
          setErro('')
        }
      })
      .catch((falha) => {
        if (ativa) setErro(mensagemDeErro(falha))
      })
      .finally(() => {
        if (ativa) setCarregando(false)
      })
    return () => {
      ativa = false
    }
  }, [atualizacao])

  function atualizar() {
    setCarregando(true)
    setAtualizacao((valor) => valor + 1)
  }

  async function registrar({ consultaId, dados }) {
    setErro('')
    setSucesso('')
    setEnviandoFalta(true)
    try {
      await registrarFalta(consultaId, dados)
      setSucesso(`Falta da consulta #${consultaId} registrada com sucesso.`)
      setCarregando(true)
      setAtualizacao((valor) => valor + 1)
      return true
    } catch (falha) {
      setErro(mensagemDeErro(falha))
      return false
    } finally {
      setEnviandoFalta(false)
    }
  }

  async function reagendar({ origemId, dados }) {
    setErro('')
    setSucesso('')
    setEnviandoReagendamento(true)
    try {
      await reagendarFalta(origemId, dados)
      setSucesso(`Reagendamento da consulta #${origemId} concluído com sucesso.`)
      setConsultaSelecionada(null)
      setCarregando(true)
      setAtualizacao((valor) => valor + 1)
      return true
    } catch (falha) {
      setErro(mensagemDeErro(falha))
      return false
    } finally {
      setEnviandoReagendamento(false)
    }
  }

  return (
    <main className="agenda-secretaria">
      <header className="agenda-cabecalho">
        <div>
          <p className="agenda-eyebrow">SECRETARIA · AGENDA</p>
          <h1 className="agenda-titulo">Agenda oficial</h1>
          <p className="agenda-subtitulo">Consultas, turnos e cadeiras da clínica-escola.</p>
        </div>
        <button className="agenda-botao-agendar" type="button" onClick={() => document.getElementById('novo-agendamento-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })}>Agendar consulta</button>
      </header>

      <TurnosDisponiveisSecretaria
        key={atualizacaoAgenda}
        aoAgendar={(slot) => setSlotInicial(slot)}
      />
      <AgendamentosSecretaria
        slotInicial={slotInicial}
        aoLimparSlot={() => setSlotInicial(null)}
        aoAlterar={() => setAtualizacaoAgenda((valor) => valor + 1)}
      />

      {sucesso && <div className="mensagem mensagem-sucesso" role="status">{sucesso}</div>}
      {erro && (
        <div className="mensagem mensagem-erro" role="alert">
          <span>{erro}</span>
          <button type="button" className="btn-link" onClick={atualizar}>Tentar novamente</button>
        </div>
      )}

      <section className="agenda-secao-faltas" aria-labelledby="agenda-faltas-titulo">
        <header className="agenda-secao-cabecalho">
          <h2 id="agenda-faltas-titulo">Registro de ausências</h2>
          <p>Registre faltas sem perder o histórico do atendimento.</p>
          <button className="btn-secundario btn-atualizar" type="button" onClick={atualizar} disabled={carregando}>Atualizar histórico</button>
        </header>
      <section className="paineis-faltas" aria-label="Registro e reagendamento de faltas">
        <PainelRegistroFalta aoRegistrar={registrar} enviando={enviandoFalta} />
        <PainelReagendamento
          key={consultaSelecionada?.consultaId ?? 'sem-consulta'}
          consulta={consultaSelecionada}
          aoReagendar={reagendar}
          aoCancelar={() => setConsultaSelecionada(null)}
          enviando={enviandoReagendamento}
        />
      </section>

      <TabelaHistoricoAusencias
        faltas={faltas}
        carregando={carregando}
        aoSelecionar={(consulta) => {
          setErro('')
          setSucesso('')
          setConsultaSelecionada(consulta)
        }}
      />

      <aside className="aviso-historico">
        <span className="aviso-icone" aria-hidden="true">i</span>
        <div>
          <strong>Histórico preservado</strong>
          <p>O reagendamento cria uma nova consulta vinculada à falta original, mantendo a rastreabilidade.</p>
        </div>
      </aside>
      </section>
    </main>
  )
}

export default AgendaSecretaria
