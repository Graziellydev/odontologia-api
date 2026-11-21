import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, ArrowLeft, CheckCircle2, ClipboardList, FileCheck2, LockKeyhole, Plus, Save, ShieldCheck, Stethoscope, X } from 'lucide-react'
import { registrarEncerramento } from '../services/retornoService.js'
import { adicionarAdendoProntuario, buscarProntuarioDoPaciente, salvarEvolucaoProntuario } from '../services/prontuarioService.js'
import './ProntuarioDigital.css'

const dataBR = (value, withTime = false) => {
  if (!value) return '—'
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value)
  if (Number.isNaN(date.getTime())) return value
  return withTime ? date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : date.toLocaleDateString('pt-BR')
}

function detalheErro(error) {
  if (!error?.status) return error?.message || 'Não foi possível concluir a operação.'
  return error.message.replace(/^HTTP \d+\s*(—\s*)?/, '').trim() || 'Acesso restrito ao prontuário.'
}

export default function ProntuarioDigital({ consulta, usuario, aoVoltar }) {
  const [historico, setHistorico] = useState(null)
  const [aba, setAba] = useState('evolucao')
  const [evolucao, setEvolucao] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [adendoAberto, setAdendoAberto] = useState(false)
  const [textoAdendo, setTextoAdendo] = useState('')
  const [salvandoAdendo, setSalvandoAdendo] = useState(false)
  const [tipoEncerramento, setTipoEncerramento] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [encerrando, setEncerrando] = useState(false)
  const [atendimentoEncerrado, setAtendimentoEncerrado] = useState(consulta.status === 'REALIZADO')
  const [bloqueio, setBloqueio] = useState('')
  const [erroOperacao, setErroOperacao] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [agora, setAgora] = useState(() => new Date())

  const alunoNome = usuario?.nome || 'Aluno'
  const identificacao = usuario?.matriculaOuRegistro || usuario?.matricula || usuario?.id || 'Registro não informado'
  const marcaDagua = useMemo(
    () => `${alunoNome} • ${identificacao} • ${agora.toLocaleString('pt-BR')}`,
    [alunoNome, identificacao, agora],
  )
  const evolucaoAtual = historico?.evolucoes?.find((item) => Number(item.consultaId) === Number(consulta.consultaId))
  const consultaRealizada = consulta.status === 'REALIZADO' || atendimentoEncerrado

  const carregarHistorico = async () => {
    const dados = await buscarProntuarioDoPaciente(consulta.pacienteId)
    setHistorico(dados)
  }

  useEffect(() => {
    let ativo = true
    setHistorico(null)
    setBloqueio('')
    setErroOperacao('')
    setAtendimentoEncerrado(consulta.status === 'REALIZADO')
    buscarProntuarioDoPaciente(consulta.pacienteId)
      .then((dados) => { if (ativo) setHistorico(dados) })
      .catch((error) => {
        if (!ativo) return
        if (error.status === 403) setBloqueio(detalheErro(error))
        else setErroOperacao(detalheErro(error))
      })
    return () => { ativo = false }
  }, [consulta.pacienteId])

  useEffect(() => {
    const timer = window.setInterval(() => setAgora(new Date()), 15000)
    return () => window.clearInterval(timer)
  }, [])

  async function salvar(event) {
    event.preventDefault()
    setErroOperacao('')
    setSalvando(true)
    try {
      await salvarEvolucaoProntuario({
        consultaId: Number(consulta.consultaId),
        procedimentoId: Number(consulta.procedimentoId),
        dente: consulta.dente || null,
        classe: consulta.classe || null,
        evolucaoClinica: evolucao.trim(),
        concluirAtendimento: false,
      })
      await carregarHistorico()
      setSucesso('Evolução clínica registrada e assinada com sucesso.')
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
      await adicionarAdendoProntuario(evolucaoAtual.id, textoAdendo.trim())
      await carregarHistorico()
      setTextoAdendo('')
      setAdendoAberto(false)
      setSucesso('Adendo incluído no histórico imutável.')
    } catch (error) {
      if (error.status === 403) setBloqueio(detalheErro(error))
      else setErroOperacao(detalheErro(error))
    } finally {
      setSalvandoAdendo(false)
    }
  }

  async function encerrar(event) {
    event.preventDefault()
    setErroOperacao('')
    if (!tipoEncerramento) return setErroOperacao('Informe como o atendimento deve ser encerrado.')
    if (tipoEncerramento === 'retorno' && !justificativa.trim()) return setErroOperacao('A justificativa técnica é obrigatória para sugerir o retorno.')
    setEncerrando(true)
    try {
      const resposta = await registrarEncerramento(consulta.consultaId, tipoEncerramento === 'retorno', justificativa.trim())
      setSucesso(resposta?.mensagem || 'Atendimento encerrado com sucesso.')
      setAtendimentoEncerrado(true)
      setTipoEncerramento('')
      setJustificativa('')
    } catch (error) {
      setErroOperacao(detalheErro(error))
    } finally {
      setEncerrando(false)
    }
  }

  const renderHistorico = () => <section className="prontuario-history-card prontuario-readonly">
    <div className="prontuario-card-heading"><div><h2>Histórico cronológico</h2><p>Registros anteriores, somente leitura</p></div></div>
    {historico?.evolucoes?.length ? historico.evolucoes.map((item) => <article className="prontuario-evolution" key={item.id}>
      <span className="prontuario-date-pill">{dataBR(item.dataRegistro, true)}</span>
      <h3>{item.procedimento || 'Procedimento clínico'}</h3>
      <p>{item.evolucaoClinica}</p>
      <small>{item.aluno || 'Aluno responsável'} · registro assinado</small>
      <span className="prontuario-signature">Assinatura Digital Temporal Não Editável</span>
      {item.adendos?.map((adendo) => <div className="prontuario-addendum-entry" key={adendo.id}>
        <b>Adendo · {dataBR(adendo.criadoEm, true)}</b><p>{adendo.texto}</p><small>{adendo.autor}</small>
      </div>)}
    </article>) : <p className="prontuario-muted">Nenhum procedimento anterior registrado.</p>}
  </section>

  return <section className="prontuario-page" aria-labelledby="prontuario-page-title">
    <div className="prontuario-page-heading">
      <button type="button" className="prontuario-back" onClick={aoVoltar}><ArrowLeft size={17} /> Voltar ao turno</button>
      <span className="prontuario-eyebrow"><LockKeyhole size={14} /> Uso clínico restrito</span>
      <h1 id="prontuario-page-title">Prontuário e evolução</h1>
      <p>{consulta.paciente} · Atendimento do turno {consulta.turno === 'MANHA' ? 'da manhã' : 'da tarde'}</p>
    </div>

    <div className="prontuario-security-banner"><ShieldCheck size={21} /><div><strong>Ambiente autorizado · sessão identificada</strong><span>Acesso registrado para o atendimento vinculado ao aluno.</span></div></div>
    {bloqueio && <div className="prontuario-page-block" role="alert"><AlertTriangle size={20} /><div><strong>Acesso restrito</strong><span>{bloqueio}</span></div><button type="button" onClick={aoVoltar}>Voltar ao turno</button></div>}
    {erroOperacao && !bloqueio && <p className="prontuario-inline-error" role="alert">{erroOperacao}</p>}
    {sucesso && <p className="prontuario-inline-success" role="status"><CheckCircle2 size={16} /> {sucesso}</p>}

    {!bloqueio && <div className="prontuario-clinical-surface">
      <div className="prontuario-watermark" aria-hidden="true">{Array.from({ length: 18 }, (_, index) => <span key={index}>{marcaDagua}</span>)}</div>
      {!historico && !erroOperacao && <p className="prontuario-muted">Carregando dados clínicos…</p>}
      {historico && <>
        <article className="prontuario-patient-summary prontuario-readonly">
          <div className="prontuario-patient-mark"><Stethoscope size={19} /></div>
          <div><h2>{historico.paciente?.nome || consulta.paciente}</h2><p>{historico.paciente?.idade ? `${historico.paciente.idade} anos · ` : ''}Prontuário {historico.paciente?.numeroProntuario || 'não informado'}</p></div>
          <dl><div><dt>Procedimento</dt><dd>{consulta.procedimento || 'Não informado'}</dd></div><div><dt>Cadeira</dt><dd>{consulta.cadeira || '—'}</dd></div><div><dt>Turno</dt><dd>{consulta.turno === 'MANHA' ? 'Manhã · 08h–12h' : 'Tarde · 13h–17h'}</dd></div></dl>
          <span className="prontuario-status-pill">{consultaRealizada ? 'REALIZADO' : (consulta.status || 'AGENDADO')}</span>
        </article>

        <div className="prontuario-tabs" role="tablist" aria-label="Seções do prontuário">
          <button type="button" role="tab" aria-selected={aba === 'evolucao'} className={aba === 'evolucao' ? 'ativo' : ''} onClick={() => setAba('evolucao')}>Evolução clínica</button>
          <button type="button" role="tab" aria-selected={aba === 'historico'} className={aba === 'historico' ? 'ativo' : ''} onClick={() => setAba('historico')}>Histórico de procedimentos</button>
        </div>

        {aba === 'evolucao' ? <>
          <div className="prontuario-evolution-grid">
            {renderHistorico()}
            <section className="prontuario-form-card">
              {!evolucaoAtual ? <form onSubmit={salvar}>
                <div className="prontuario-card-heading"><div><h2>Registrar evolução</h2><p>O registro será assinado e ficará imutável após salvar.</p></div></div>
                <div className="prontuario-field-grid"><label>Procedimento<input value={consulta.procedimento || ''} readOnly /></label><label>Elemento / região<input value={consulta.dente || 'Não se aplica'} readOnly /></label></div>
                <label>Descrição clínica<textarea required minLength={1} maxLength={20000} rows={8} value={evolucao} onChange={(event) => setEvolucao(event.target.value)} placeholder="Descreva os procedimentos realizados e as observações clínicas…" /></label>
                <p className="prontuario-signature-note"><LockKeyhole size={15} /> A assinatura digital temporal será gerada no servidor.</p>
                <button className="prontuario-primary" type="submit" disabled={salvando || !evolucao.trim() || !consulta.procedimentoId}><Save size={16} /> {salvando ? 'Salvando…' : 'Salvar evolução'}</button>
              </form> : <div className="prontuario-saved-card"><FileCheck2 size={22} /><div><strong>Evolução assinada</strong><span>Assinatura Digital Temporal Não Editável</span><small>{dataBR(evolucaoAtual.dataRegistro, true)} · {evolucaoAtual.aluno || alunoNome}</small><button className="prontuario-text-button" type="button" onClick={() => setAdendoAberto(true)}><Plus size={15} /> Adicionar adendo</button></div></div>}
            </section>
          </div>
          {evolucaoAtual && !consultaRealizada && <form className="prontuario-close-card" onSubmit={encerrar}>
            <div><span className="prontuario-eyebrow"><ClipboardList size={14} /> Encerramento do atendimento</span><h2>Qual o próximo passo clínico?</h2><p>Esta ação usa o fluxo de retorno já validado para o atendimento.</p></div>
            <div className="prontuario-close-options"><label><input type="radio" name="encerramento" checked={tipoEncerramento === 'retorno'} onChange={() => setTipoEncerramento('retorno')} /><span><b>Necessita Retorno</b><small>Preserva a demanda para a fila da secretaria.</small></span></label><label><input type="radio" name="encerramento" checked={tipoEncerramento === 'concluido'} onChange={() => setTipoEncerramento('concluido')} /><span><b>Tratamento Concluído</b><small>Encerra sem criar retorno.</small></span></label></div>
            {tipoEncerramento === 'retorno' && <label className="prontuario-justificativa">Justificativa técnica *<textarea rows={3} maxLength={1000} value={justificativa} onChange={(event) => setJustificativa(event.target.value)} placeholder="Ex.: Canal instrumentado, falta obturação." /></label>}
            <button className="prontuario-primary" type="submit" disabled={encerrando || !tipoEncerramento || (tipoEncerramento === 'retorno' && !justificativa.trim())}>{encerrando ? 'Registrando…' : 'Confirmar encerramento'}</button>
          </form>}
        </> : <div className="prontuario-procedure-grid">{renderHistorico()}<section className="prontuario-history-card prontuario-readonly"><div className="prontuario-card-heading"><div><h2>Anamnese e odontograma</h2><p>Resumo clínico atual</p></div></div><h3>Anamnese</h3><p>{historico.anamnese?.queixaPrincipal || 'Sem queixa principal registrada.'}</p>{historico.anamnese?.historiaQueixa && <p>{historico.anamnese.historiaQueixa}</p>}<h3>Odontograma</h3>{historico.odontograma?.length ? <ul className="prontuario-tooth-list">{historico.odontograma.map((item) => <li key={item.id}><b>{item.dente}{item.faceDente ? ` · ${item.faceDente}` : ''}</b><span>{item.condicaoDente}</span><small>{item.observacao || 'Sem observação'}</small></li>)}</ul> : <p className="prontuario-muted">Odontograma sem registros.</p>}</section></div>}
      </>}
    </div>}

    {adendoAberto && <div className="prontuario-overlay prontuario-overlay-mini" onMouseDown={(event) => { if (event.target === event.currentTarget) setAdendoAberto(false) }}><form className="prontuario-mini-modal" role="dialog" aria-modal="true" aria-label="Adicionar adendo" onSubmit={salvarAdendo}><button className="prontuario-icon-button" type="button" onClick={() => setAdendoAberto(false)} aria-label="Fechar"><X size={18} /></button><span className="prontuario-modal-icon"><Plus size={20} /></span><h2>Adicionar adendo</h2><p>O adendo complementa o registro assinado e não altera a evolução original.</p><label>Texto do adendo<textarea required maxLength={20000} rows={4} value={textoAdendo} onChange={(event) => setTextoAdendo(event.target.value)} placeholder="Informe a complementação clínica…" /></label><div className="prontuario-modal-actions"><button className="prontuario-secondary" type="button" onClick={() => setAdendoAberto(false)}>Cancelar</button><button className="prontuario-primary" type="submit" disabled={salvandoAdendo || !textoAdendo.trim()}>{salvandoAdendo ? 'Salvando…' : 'Salvar adendo'}</button></div></form></div>}
  </section>
}
