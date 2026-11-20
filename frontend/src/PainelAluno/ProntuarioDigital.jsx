import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, CalendarDays, ClipboardList, FileCheck2, LockKeyhole, Plus, ShieldCheck, X } from 'lucide-react'
import { buscarAtendimentosDeHoje } from '../services/retornoService.js'
import { adicionarAdendoProntuario, buscarProntuarioDoPaciente, salvarEvolucaoProntuario } from '../services/prontuarioService.js'
import './ProntuarioDigital.css'

const dataBR = (value) => {
  if (!value) return '—'
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
}

function detalheErro(error) {
  if (!error?.status) return error?.message || 'Não foi possível acessar o prontuário.'
  return error.message.replace(/^HTTP \d+\s*(—\s*)?/, '').trim() || 'Acesso restrito ao prontuário.'
}

export default function ProntuarioDigital({ usuario }) {
  const [atendimentos, setAtendimentos] = useState([])
  const [carregandoTurno, setCarregandoTurno] = useState(true)
  const [erroTurno, setErroTurno] = useState('')
  const [consulta, setConsulta] = useState(null)
  const [historico, setHistorico] = useState(null)
  const [aba, setAba] = useState('evolucao')
  const [evolucao, setEvolucao] = useState('')
  const [concluirAtendimento, setConcluirAtendimento] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [adendoAberto, setAdendoAberto] = useState(false)
  const [textoAdendo, setTextoAdendo] = useState('')
  const [salvandoAdendo, setSalvandoAdendo] = useState(false)
  const [bloqueio, setBloqueio] = useState('')
  const [erroOperacao, setErroOperacao] = useState('')
  const [agora, setAgora] = useState(() => new Date())

  const alunoNome = usuario?.nome || 'Aluno'
  const identificacao = usuario?.matriculaOuRegistro || usuario?.matricula || 'Matrícula não informada'
  const marcaDagua = useMemo(() => `${alunoNome} • ${identificacao} • ${agora.toLocaleString('pt-BR')}`, [alunoNome, identificacao, agora])

  useEffect(() => {
    let ativo = true
    buscarAtendimentosDeHoje()
      .then((dados) => { if (ativo) { setAtendimentos(Array.isArray(dados) ? dados : []); setErroTurno('') } })
      .catch((error) => { if (ativo) setErroTurno(detalheErro(error)) })
      .finally(() => { if (ativo) setCarregandoTurno(false) })
    return () => { ativo = false }
  }, [])

  useEffect(() => {
    const timer = window.setInterval(() => setAgora(new Date()), 15000)
    return () => window.clearInterval(timer)
  }, [])

  async function abrirProntuario(atendimento) {
    setConsulta(atendimento)
    setHistorico(null)
    setAba('evolucao')
    setErroOperacao('')
    setEvolucao('')
    setConcluirAtendimento(false)
    try {
      const dados = await buscarProntuarioDoPaciente(atendimento.pacienteId)
      setHistorico(dados)
    } catch (error) {
      if (error.status === 403) setBloqueio(detalheErro(error))
      else setErroOperacao(detalheErro(error))
      setConsulta(null)
    }
  }

  async function atualizarHistorico() {
    if (!consulta) return
    const dados = await buscarProntuarioDoPaciente(consulta.pacienteId)
    setHistorico(dados)
  }

  async function salvar(event) {
    event.preventDefault()
    setErroOperacao('')
    setSalvando(true)
    try {
      const salvo = await salvarEvolucaoProntuario({
        consultaId: Number(consulta.consultaId),
        procedimentoId: Number(consulta.procedimentoId),
        dente: consulta.dente || null,
        classe: consulta.classe || null,
        evolucaoClinica: evolucao.trim(),
        concluirAtendimento,
      })
      if (salvo?.atendimentoConcluido) {
        setConsulta((atual) => atual ? { ...atual, status: 'REALIZADO' } : atual)
      }
      await atualizarHistorico()
      setAba('historico')
    } catch (error) {
      if (error.status === 403) setBloqueio(detalheErro(error))
      else setErroOperacao(detalheErro(error))
    } finally {
      setSalvando(false)
    }
  }

  async function salvarAdendo(event) {
    event.preventDefault()
    setSalvandoAdendo(true)
    setErroOperacao('')
    try {
      const ultimo = historico?.evolucoes?.[0]
      if (!ultimo) throw new Error('Salve uma evolução antes de adicionar um adendo.')
      await adicionarAdendoProntuario(ultimo.id, textoAdendo.trim())
      await atualizarHistorico()
      setTextoAdendo('')
      setAdendoAberto(false)
    } catch (error) {
      if (error.status === 403) setBloqueio(detalheErro(error))
      else setErroOperacao(detalheErro(error))
    } finally {
      setSalvandoAdendo(false)
    }
  }

  function fecharProntuario() {
    setConsulta(null)
    setHistorico(null)
    setAdendoAberto(false)
  }

  return <>
    <section className="prontuario-turno" aria-labelledby="prontuario-turno-titulo">
      <div className="prontuario-section-heading">
        <div>
          <span className="prontuario-eyebrow"><ShieldCheck size={15} /> Atendimento vinculado</span>
          <h2 id="prontuario-turno-titulo">Pacientes do turno</h2>
          <p>Abra o prontuário do paciente associado à sua consulta de hoje.</p>
        </div>
      </div>
      {carregandoTurno && <p className="prontuario-muted">Carregando atendimentos do turno…</p>}
      {!carregandoTurno && erroTurno && <p className="prontuario-inline-error" role="alert">{erroTurno}</p>}
      {!carregandoTurno && !erroTurno && atendimentos.length === 0 && (
        <div className="prontuario-empty">Você não tem pacientes vinculados a consultas de hoje.</div>
      )}
      {!!atendimentos.length && <div className="prontuario-appointments">
        {atendimentos.map((item) => <article className="prontuario-appointment" key={item.consultaId}>
          <div className="prontuario-patient-mark" aria-hidden="true">{item.paciente?.slice(0, 1) || 'P'}</div>
          <div className="prontuario-appointment-info">
            <strong>{item.paciente}</strong>
            <span>{item.procedimento}{item.dente ? ` · Dente ${item.dente}` : ''}{item.classe ? ` · Classe ${item.classe}` : ''}</span>
            <small><CalendarDays size={13} /> {dataBR(item.data)} · {item.turno === 'MANHA' ? 'Manhã' : 'Tarde'}</small>
          </div>
          <button className="prontuario-primary" type="button" onClick={() => abrirProntuario(item)}>
            <ClipboardList size={16} /> Abrir prontuário
          </button>
        </article>)}
      </div>}
    </section>

    {consulta && <div className="prontuario-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) fecharProntuario() }}>
      <section className="prontuario-drawer" role="dialog" aria-modal="true" aria-label={`Prontuário de ${consulta.paciente}`}>
        <header className="prontuario-drawer-header">
          <div><span className="prontuario-eyebrow"><LockKeyhole size={14} /> Uso clínico restrito</span><h2>Prontuário e evolução</h2>
            <p>{consulta.paciente} · {consulta.procedimento || 'Procedimento não informado'} · {dataBR(consulta.data)}, {consulta.turno === 'MANHA' ? 'manhã' : 'tarde'}</p>
          </div>
          <button className="prontuario-icon-button" type="button" onClick={fecharProntuario} aria-label="Fechar prontuário"><X /></button>
        </header>
        <div className="prontuario-security-banner"><ShieldCheck size={20} /><div><strong>Ambiente autorizado · sessão identificada</strong><span>Acesso registrado para o atendimento vinculado ao aluno.</span></div></div>
        <div className="prontuario-tabs" role="tablist" aria-label="Seções do prontuário">
          <button type="button" role="tab" aria-selected={aba === 'evolucao'} className={aba === 'evolucao' ? 'ativo' : ''} onClick={() => setAba('evolucao')}>Evolução clínica</button>
          <button type="button" role="tab" aria-selected={aba === 'historico'} className={aba === 'historico' ? 'ativo' : ''} onClick={() => setAba('historico')}>Histórico de procedimentos</button>
        </div>
        <div className="prontuario-drawer-content clinical-content">
          <div className="prontuario-watermark" aria-hidden="true">{Array.from({ length: 8 }, (_, index) => <span key={index}>{marcaDagua}</span>)}</div>
          {!historico && <p className="prontuario-muted">Carregando dados clínicos…</p>}
          {historico && <>
            {aba === 'evolucao' ? <div className="prontuario-workspace">
              <aside className="prontuario-summary-card">
                <h3>{historico.paciente?.nome || consulta.paciente}</h3>
                <p>{historico.paciente?.idade ? `${historico.paciente.idade} anos · ` : ''}Prontuário {historico.paciente?.numeroProntuario || 'não informado'}</p>
                <hr />
                <strong>{consulta.procedimento || 'Procedimento não informado'}</strong>
                <span>{consulta.turno === 'MANHA' ? 'Manhã · 08h às 12h' : 'Tarde · 13h às 17h'} · Cadeira {consulta.cadeira || '—'}</span>
                <span className="prontuario-status-pill">{consulta.status || 'Em atendimento'}</span>
                <div className="prontuario-anamnese-compact"><b>Anamnese resumida</b><p>{historico.anamnese?.queixaPrincipal || 'Sem queixa principal registrada.'}</p></div>
              </aside>
              <section className="prontuario-form-card">
                {!historico.evolucoes?.some((item) => Number(item.consultaId) === Number(consulta.consultaId)) ? <form onSubmit={salvar}>
                  <div className="prontuario-card-heading"><div><h3>Registrar evolução</h3><p>O registro será assinado e ficará imutável após salvar.</p></div></div>
                  <div className="prontuario-field-grid">
                    <label>Procedimento<input value={consulta.procedimento || ''} readOnly /></label>
                    <label>Dente (FDI)<input value={consulta.dente || 'Não se aplica'} readOnly /></label>
                  </div>
                  <label>Descrição clínica<textarea required minLength={1} maxLength={20000} rows={7} value={evolucao} onChange={(event) => setEvolucao(event.target.value)} placeholder="Descreva os procedimentos realizados e as observações clínicas…" /></label>
                  <label className="prontuario-checkbox"><input type="checkbox" checked={concluirAtendimento} onChange={(event) => setConcluirAtendimento(event.target.checked)} /> Tratamento concluído · encerrar atendimento</label>
                  {erroOperacao && <p className="prontuario-inline-error" role="alert">{erroOperacao}</p>}
                  <button className="prontuario-primary" type="submit" disabled={salvando || !evolucao.trim() || !consulta.procedimentoId}>
                    <FileCheck2 size={16} /> {salvando ? 'Salvando…' : 'Salvar evolução e assinar'}
                  </button>
                </form> : <div className="prontuario-saved-card"><FileCheck2 size={20} /><div><strong>Evolução assinada</strong><span>Assinatura Digital Temporal Não Editável</span><button className="prontuario-text-button" type="button" onClick={() => setAdendoAberto(true)}><Plus size={15} /> Adicionar Adendo</button></div></div>}
              </section>
            </div> : <div className="prontuario-history-layout">
              <section className="prontuario-history-card"><div className="prontuario-card-heading"><div><h3>Histórico cronológico</h3><p>Registros anteriores, somente leitura</p></div></div>
                {historico.evolucoes?.length ? historico.evolucoes.map((item) => <article className="prontuario-evolution" key={item.id}>
                  <span className="prontuario-date-pill">{dataBR(item.dataRegistro)}</span><h4>{item.procedimento}</h4>
                  <p>{item.evolucaoClinica}</p><small>{item.aluno} · registro assinado</small>
                  <span className="prontuario-signature">Assinatura Digital Temporal Não Editável</span>
                  {item.adendos?.map((adendo) => <div className="prontuario-addendum-entry" key={adendo.id}><b>Adendo · {dataBR(adendo.criadoEm)}</b><p>{adendo.texto}</p><small>{adendo.autor}</small></div>)}
                </article>) : <p className="prontuario-muted">Nenhum procedimento anterior registrado.</p>}
              </section>
              <section className="prontuario-history-card"><div className="prontuario-card-heading"><div><h3>Anamnese e odontograma</h3><p>Resumo clínico atual</p></div></div>
                <h4>Anamnese</h4><p>{historico.anamnese?.queixaPrincipal || 'Sem queixa principal registrada.'}</p>
                {historico.anamnese?.historiaQueixa && <p>{historico.anamnese.historiaQueixa}</p>}
                <h4>Odontograma</h4>
                {historico.odontograma?.length ? <ul className="prontuario-tooth-list">{historico.odontograma.map((item) => <li key={item.id}><b>{item.dente}{item.faceDente ? ` · ${item.faceDente}` : ''}</b><span>{item.condicaoDente}</span><small>{item.observacao || 'Sem observação'}</small></li>)}</ul> : <p className="prontuario-muted">Odontograma sem registros.</p>}
              </section>
            </div>}
          </>}
        </div>
        <footer className="prontuario-drawer-footer"><span>Clínica-escola · dados protegidos</span><button type="button" onClick={fecharProntuario}>Fechar</button></footer>
      </section>
    </div>}

    {adendoAberto && <div className="prontuario-overlay prontuario-overlay-mini" onMouseDown={(event) => { if (event.target === event.currentTarget) setAdendoAberto(false) }}>
      <form className="prontuario-mini-modal" role="dialog" aria-modal="true" aria-label="Adicionar adendo" onSubmit={salvarAdendo}>
        <button className="prontuario-icon-button" type="button" onClick={() => setAdendoAberto(false)} aria-label="Fechar"><X size={18} /></button>
        <span className="prontuario-modal-icon"><Plus size={20} /></span><h2>Adicionar Adendo</h2>
        <p>O adendo complementa o registro assinado e não altera a evolução original.</p>
        <label>Texto do adendo<textarea required maxLength={20000} rows={4} value={textoAdendo} onChange={(event) => setTextoAdendo(event.target.value)} placeholder="Informe a complementação clínica…" /></label>
        {erroOperacao && <p className="prontuario-inline-error" role="alert">{erroOperacao}</p>}
        <div className="prontuario-modal-actions"><button className="prontuario-secondary" type="button" onClick={() => setAdendoAberto(false)}>Cancelar</button><button className="prontuario-primary" type="submit" disabled={salvandoAdendo || !textoAdendo.trim()}>{salvandoAdendo ? 'Salvando…' : 'Salvar adendo'}</button></div>
      </form>
    </div>}

    {bloqueio && <div className="prontuario-overlay prontuario-overlay-block">
      <section className="prontuario-block-modal" role="alertdialog" aria-modal="true" aria-labelledby="prontuario-block-title">
        <button className="prontuario-icon-button" type="button" onClick={() => setBloqueio('')} aria-label="Fechar"><X size={18} /></button>
        <span className="prontuario-block-icon"><AlertTriangle size={20} /></span><h2 id="prontuario-block-title">Acesso restrito</h2>
        <p>{bloqueio}</p><button className="prontuario-danger-button" type="button" onClick={() => setBloqueio('')}>Entendi</button>
      </section>
    </div>}
  </>
}
