import { useCallback, useEffect, useMemo, useState } from 'react'
import { CheckCircle2, ChevronLeft, ChevronRight, ExternalLink, MessageCircle, RefreshCw, Send } from 'lucide-react'
import { listarNotificacoesWhatsApp, processarLembretes24h, simularRespostaWhatsapp } from '../services/agendamentoService.js'
import './ComunicacaoWhatsapp.css'

const POR_PAGINA = 6
const TIPOS = {
  LEMBRETE_24H: 'Lembrete 24h',
  AVISO_CANCELAMENTO: 'Cancelamento imediato',
  AVISO_REMARCACAO: 'Remarcação imediata',
  OFERTA_VAGA_DESISTENCIA: 'Chamada rápida',
  PESQUISA_SATISFACAO: 'Satisfação opcional',
  CONVITE_REAGENDAMENTO: 'Convite de remarcação',
  COMPROVANTE_AGENDAMENTO: 'Confirmação',
}

function dataHora(valor) {
  if (!valor) return 'Data não disponível'
  return new Date(valor).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function dataLonga(valor) {
  if (!valor) return 'Data não disponível'
  return new Date(valor).toLocaleDateString('pt-BR')
}

function ComunicacaoWhatsapp() {
  const [inicio, setInicio] = useState('')
  const [fim, setFim] = useState('')
  const [tipo, setTipo] = useState('')
  const [status, setStatus] = useState('')
  const [itens, setItens] = useState([])
  const [selecionadaId, setSelecionadaId] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [carregando, setCarregando] = useState(false)
  const [processando, setProcessando] = useState(false)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [atualizado, setAtualizado] = useState(null)

  const consultar = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const resultado = await listarNotificacoesWhatsApp({ inicio, fim, tipo, status })
      setItens(Array.isArray(resultado) ? resultado : [])
      setAtualizado(new Date())
      setPagina(0)
    } catch (falha) {
      setErro(falha?.message || 'Não foi possível carregar as notificações.')
      setItens([])
    } finally {
      setCarregando(false)
    }
  }, [inicio, fim, tipo, status])

  useEffect(() => { consultar() }, [consultar])

  useEffect(() => {
    if (!itens.some((item) => item.id === selecionadaId)) {
      setSelecionadaId(itens[0]?.id ?? null)
    }
  }, [itens, selecionadaId])

  const selecionada = useMemo(
    () => itens.find((item) => item.id === selecionadaId) || null,
    [itens, selecionadaId],
  )
  const totalPaginas = Math.max(1, Math.ceil(itens.length / POR_PAGINA))
  const exibidas = itens.slice(pagina * POR_PAGINA, (pagina + 1) * POR_PAGINA)
  const chamadas = itens.filter((item) => item.tipo === 'OFERTA_VAGA_DESISTENCIA')
  const chamada = chamadas[0] || null
  const demandaCount = chamada
    ? chamadas.filter((item) => item.consultaId === chamada.consultaId).length
    : 0
  const permiteResposta = selecionada && ['LEMBRETE_24H', 'OFERTA_VAGA_DESISTENCIA'].includes(selecionada.tipo)

  async function gerarLembretes() {
    setProcessando(true)
    setErro('')
    setAviso('')
    try {
      const resultado = await processarLembretes24h()
      setAviso((resultado?.criadas ?? 0) + ' lembrete(s) preparado(s). Nenhuma mensagem foi enviada automaticamente.')
      await consultar()
    } catch (falha) {
      setErro(falha?.message || 'Não foi possível gerar os lembretes.')
    } finally {
      setProcessando(false)
    }
  }

  async function simular(mensagem) {
    if (!selecionada?.telefonePaciente) return
    setErro('')
    setAviso('Simulação de teste enviada ao webhook; não é uma mensagem real do WhatsApp.')
    try {
      await simularRespostaWhatsapp(selecionada.telefonePaciente, mensagem)
      await consultar()
    } catch (falha) {
      setAviso('')
      setErro(falha?.message || 'Não foi possível simular a resposta.')
    }
  }

  return (
    <main className="com-whatsapp">
      <header className="cw-header">
        <div>
          <span className="cw-eyebrow">ÁREA DA SECRETARIA</span>
          <h1>Comunicação WhatsApp</h1>
          <p>Acompanhe notificações automáticas e respostas dos pacientes pela secretaria</p>
        </div>
        <button className="cw-button cw-primary" type="button" onClick={gerarLembretes} disabled={processando}>
          <RefreshCw size={16} className={processando ? 'cw-spin' : ''} />
          {processando ? 'Gerando…' : 'Gerar lembretes 24h'}
        </button>
      </header>

      <section className="cw-filters" aria-label="Filtros de notificações">
        <label className="cw-period">Período
          <span><input aria-label="Data inicial" type="date" value={inicio} onChange={(event) => setInicio(event.target.value)} />
          <span aria-hidden="true">–</span>
          <input aria-label="Data final" type="date" value={fim} onChange={(event) => setFim(event.target.value)} /></span>
        </label>
        <label>Tipo
          <select value={tipo} onChange={(event) => setTipo(event.target.value)}>
            <option value="">Todos os tipos</option>
            {Object.entries(TIPOS).map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
          </select>
        </label>
        <label>Status
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">Todos os status</option>
            <option value="PENDENTE">Pendente</option>
            <option value="ENVIADO">Enviado</option>
            <option value="CONFIRMADO_RESPOSTA">Resposta recebida</option>
            <option value="ERRO">Erro</option>
          </select>
        </label>
      </section>

      {erro && <p className="cw-feedback cw-error" role="alert">{erro}</p>}
      {aviso && <p className="cw-feedback cw-notice" role="status">{aviso}</p>}

      <section className="cw-grid">
        <div className="cw-left">
          <article className="cw-card cw-table-card">
            <header className="cw-card-heading">
              <div><h2>Mensagens automáticas</h2><p>Não há portal ou área de acesso do paciente</p></div>
              <button type="button" className="cw-icon-button" aria-label="Atualizar mensagens" onClick={consultar} disabled={carregando}><RefreshCw size={15} /></button>
            </header>
            <div className="cw-table-wrap">
              <table>
                <thead><tr><th>Paciente / envio</th><th>Tipo</th><th>Status</th></tr></thead>
                <tbody>
                  {carregando && <tr><td colSpan="3" className="cw-empty">Carregando mensagens…</td></tr>}
                  {!carregando && exibidas.length === 0 && <tr><td colSpan="3" className="cw-empty">Nenhuma notificação corresponde aos filtros.</td></tr>}
                  {!carregando && exibidas.map((item) => (
                    <tr key={item.id} className={item.id === selecionadaId ? 'cw-selected' : ''} onClick={() => setSelecionadaId(item.id)}>
                      <td><strong>{item.paciente}</strong><small>{dataHora(item.criadaEm)}</small></td>
                      <td>{TIPOS[item.tipo] || item.tipo}</td>
                      <td><span className={'cw-status ' + (item.status === 'CONFIRMADO_RESPOSTA' ? 'cw-status-ok' : item.status === 'ERRO' ? 'cw-status-error' : '')}>{item.status === 'CONFIRMADO_RESPOSTA' ? (item.respostaRecebida === '1' ? 'Confirmada · resposta 1' : item.respostaRecebida === '2' ? 'Cancelada · resposta 2' : 'Respondida') : item.status === 'PENDENTE' ? 'Aguardando resposta' : item.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <footer className="cw-pagination">
              <span>{itens.length ? (pagina * POR_PAGINA + 1) + '–' + Math.min((pagina + 1) * POR_PAGINA, itens.length) + ' de ' + itens.length + ' envios' : '0 envios'}{atualizado ? ' · atualização às ' + atualizado.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : ''}</span>
              <div><button type="button" aria-label="Página anterior" disabled={pagina === 0} onClick={() => setPagina((valor) => Math.max(0, valor - 1))}><ChevronLeft size={15} /></button><span>{pagina + 1}</span><button type="button" aria-label="Próxima página" disabled={pagina + 1 >= totalPaginas} onClick={() => setPagina((valor) => Math.min(totalPaginas - 1, valor + 1))}><ChevronRight size={15} /></button></div>
            </footer>
          </article>

          <article className="cw-card cw-call-card">
            <header><MessageCircle size={16} /><h2>Chamada rápida · vaga de hoje</h2></header>
            {!chamada ? <p className="cw-no-call">Nenhuma chamada rápida registrada para o período filtrado.</p> : <>
              <dl className="cw-call-details">
                <div><dt>Vaga liberada</dt><dd>{dataLonga(chamada.dataConsulta)} · {chamada.turno === 'MANHA' ? 'manhã' : chamada.turno === 'TARDE' ? 'tarde' : 'turno indisponível'} · {chamada.cadeira || 'cadeira indisponível'}</dd></div>
                <div><dt>Compatibilidade</dt><dd>{chamada.procedimento || 'Procedimento indisponível'} · dente {chamada.dente || '—'} · classe {chamada.classe || '—'}</dd></div>
                <div><dt>Situação</dt><dd>{demandaCount} demanda(s) compatível(is) notificada(s)</dd></div>
              </dl>
              <div className="cw-alert"><strong>A primeira aceitação compatível ocupa a vaga</strong><p>Os demais recebem aviso de indisponibilidade. Sem compatíveis, a vaga continua disponível e a secretaria é avisada.</p></div>
            </>}
          </article>
        </div>

        <article className="cw-card cw-preview">
          <header className="cw-card-heading"><div><h2>Prévia da conversa</h2><p>{selecionada ? selecionada.paciente + ' · ' + (TIPOS[selecionada.tipo] || selecionada.tipo) : 'Selecione uma notificação'}</p></div></header>
          {!selecionada ? <div className="cw-empty cw-preview-empty">A prévia aparecerá quando uma mensagem for selecionada.</div> : <>
            <span className="cw-wa-badge"><MessageCircle size={13} /> WhatsApp · acompanhamento de envio</span>
            <div className="cw-chat">
              <small className="cw-chat-date">{dataHora(selecionada.criadaEm)}</small>
              <div className="cw-bubble cw-outgoing">{selecionada.conteudo}<small>{selecionada.status === 'ENVIADO' ? 'Envio registrado' : 'Pronta para envio manual'}</small></div>
              {selecionada.respostaRecebida && <div className="cw-bubble cw-incoming">{selecionada.respostaRecebida}<small>{selecionada.respondidaEm ? dataHora(selecionada.respondidaEm) : 'Resposta registrada'}</small></div>}
            </div>
            <div className="cw-actions">
              <a className="cw-button cw-whatsapp" href={selecionada.linkWhatsapp || undefined} target="_blank" rel="noreferrer" aria-disabled={!selecionada.linkWhatsapp}>
                <ExternalLink size={15} /> Abrir no WhatsApp
              </a>
              <div>
                <button className="cw-button cw-simulate" type="button" disabled={!permiteResposta || !selecionada.telefonePaciente} onClick={() => simular('1')}><Send size={14} /> Simular 1 · Confirmar</button>
                <button className="cw-button cw-simulate" type="button" disabled={!permiteResposta || !selecionada.telefonePaciente} onClick={() => simular('2')}><Send size={14} /> Simular 2 · Cancelar</button>
              </div>
              <small className="cw-simulation-note">Simulações de teste; não são respostas recebidas diretamente do WhatsApp.</small>
            </div>
            {selecionada.respostaRecebida === '1' && selecionada.tipo === 'LEMBRETE_24H' && <div className="cw-confirmed"><CheckCircle2 size={17} /><div><strong>Confirmação registrada na agenda</strong><small>{selecionada.respondidaEm ? dataHora(selecionada.respondidaEm) : 'Resposta recebida: 1'}</small></div></div>}
          </>}
        </article>
      </section>
    </main>
  )
}

export default ComunicacaoWhatsapp
