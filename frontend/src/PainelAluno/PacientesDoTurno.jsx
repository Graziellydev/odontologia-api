import { useEffect, useState } from 'react'
import { CalendarDays, ClipboardList, ShieldCheck } from 'lucide-react'
import { buscarAtendimentosDeHoje } from '../services/retornoService.js'
import './ProntuarioDigital.css'

const dataBR = (value) => {
  if (!value) return '—'
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
}

function detalheErro(error) {
  if (!error?.status) return error?.message || 'Não foi possível carregar os atendimentos do turno.'
  return error.message.replace(/^HTTP \d+\s*(—\s*)?/, '').trim()
}

export default function PacientesDoTurno({ aoAbrirProntuario }) {
  const [estado, setEstado] = useState({ carregando: true, erro: '', atendimentos: [] })

  useEffect(() => {
    let ativo = true
    buscarAtendimentosDeHoje()
      .then((dados) => ativo && setEstado({ carregando: false, erro: '', atendimentos: Array.isArray(dados) ? dados : [] }))
      .catch((erro) => ativo && setEstado({ carregando: false, erro: detalheErro(erro), atendimentos: [] }))
    return () => { ativo = false }
  }, [])

  return <section className="prontuario-turno" aria-labelledby="prontuario-turno-titulo">
    <div className="prontuario-section-heading">
      <div>
        <span className="prontuario-eyebrow"><ShieldCheck size={15} /> Atendimento vinculado</span>
        <h1 id="prontuario-turno-titulo">Pacientes do turno</h1>
        <p>Abra o prontuário do paciente associado à sua consulta de hoje.</p>
      </div>
    </div>
    {estado.carregando && <p className="prontuario-muted">Carregando atendimentos do turno…</p>}
    {!estado.carregando && estado.erro && <p className="prontuario-inline-error" role="alert">{estado.erro}</p>}
    {!estado.carregando && !estado.erro && estado.atendimentos.length === 0 && <div className="prontuario-empty">Você não tem pacientes vinculados a consultas de hoje.</div>}
    {!!estado.atendimentos.length && <div className="prontuario-appointments">
      {estado.atendimentos.map((item) => <article className="prontuario-appointment" key={item.consultaId}>
        <div className="prontuario-patient-mark" aria-hidden="true">{item.paciente?.slice(0, 1) || 'P'}</div>
        <div className="prontuario-appointment-info">
          <strong>{item.paciente}</strong>
          <span>{item.procedimento}{item.dente ? ` · Dente ${item.dente}` : ''}{item.classe ? ` · Classe ${item.classe}` : ''}</span>
          <small><CalendarDays size={13} /> {dataBR(item.data)} · {item.turno === 'MANHA' ? 'Manhã' : 'Tarde'}</small>
        </div>
        <button className="prontuario-primary" type="button" onClick={() => aoAbrirProntuario(item)}>
          <ClipboardList size={16} /> Abrir prontuário
        </button>
      </article>)}
    </div>}
  </section>
}
