import { useState } from 'react'

function PainelReagendamento({ consulta, aoReagendar, aoCancelar, enviando = false }) {
  const [novaVagaId, setNovaVagaId] = useState('')
  const [notificarWhatsapp, setNotificarWhatsapp] = useState(true)

  if (!consulta) {
    return (
      <section className="painel painel-reagendamento painel-reagendamento-vazio" aria-labelledby="titulo-reagendamento">
        <div className="painel-cabecalho">
          <h2 className="painel-titulo" id="titulo-reagendamento">Reagendar consulta</h2>
          <p className="painel-subtitulo">Selecione uma ausência no histórico para iniciar o reagendamento.</p>
        </div>
        <div className="aviso-informativo">
          <span className="aviso-icone" aria-hidden="true">i</span>
          <p>O reagendamento cria uma nova consulta e preserva o registro original da falta.</p>
        </div>
      </section>
    )
  }

  async function enviar(evento) {
    evento.preventDefault()
    if (!novaVagaId) return
    const reagendado = await aoReagendar({
      origemId: consulta.consultaId,
      dados: {
        novaVagaId: Number(novaVagaId),
        notificarWhatsapp,
      },
    })
    if (!reagendado) return

    setNovaVagaId('')
  }

  const nome = consulta.vinculoPaciente || consulta.vinculoAluno || 'Vínculo não informado'

  return (
    <form className="painel painel-reagendamento" onSubmit={enviar}>
      <div className="painel-cabecalho">
        <h2 className="painel-titulo">Reagendar consulta</h2>
        <p className="painel-subtitulo">Informe a nova vaga para a consulta selecionada.</p>
      </div>

      <span className="status-falta">Falta registrada</span>
      <h3 className="reagendamento-paciente">Consulta #{consulta.consultaId}</h3>
      <p className="reagendamento-detalhe">
        {nome}{consulta.data ? ` · ${consulta.data}` : ''}{consulta.ausencia ? ` · Faltou: ${consulta.ausencia.toLowerCase()}` : ''}
      </p>

      <label className="campo">
        <span className="campo-label">Nova Vaga ID</span>
        <input
          className="campo-controle"
          type="number"
          min="1"
          required
          value={novaVagaId}
          onChange={(evento) => setNovaVagaId(evento.target.value)}
          placeholder="Informe o ID da vaga disponível"
        />
      </label>

      <label className="campo-checkbox">
        <input
          type="checkbox"
          checked={notificarWhatsapp}
          onChange={(evento) => setNotificarWhatsapp(evento.target.checked)}
        />
        <span>Notificar pelo WhatsApp</span>
      </label>

      <div className="aviso-informativo">
        <span className="aviso-icone" aria-hidden="true">i</span>
        <p>A consulta original será mantida no histórico para auditoria.</p>
      </div>

      <div className="acoes-painel">
        <button className="btn-secundario" type="button" onClick={aoCancelar} disabled={enviando}>
          Cancelar
        </button>
        <button className="btn-primario" type="submit" disabled={enviando || !novaVagaId}>
          {enviando ? 'Reagendando...' : 'Confirmar reagendamento'}
        </button>
      </div>
    </form>
  )
}

export default PainelReagendamento
