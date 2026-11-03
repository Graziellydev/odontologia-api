import { useState } from 'react'

function PainelRegistroFalta({ aoRegistrar, enviando = false }) {
  const [consultaId, setConsultaId] = useState('')
  const [quemFaltou, setQuemFaltou] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [justificada, setJustificada] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    if (!consultaId || !quemFaltou) return

    const registrado = await aoRegistrar({
      consultaId: Number(consultaId),
      dados: {
        quemFaltou,
        justificada,
        justificativa: justificativa.trim(),
      },
    })
    if (!registrado) return

    setConsultaId('')
    setQuemFaltou('')
    setJustificativa('')
    setJustificada(false)
  }

  return (
    <form className="painel painel-registro" onSubmit={enviar}>
      <div className="painel-cabecalho">
        <h2 className="painel-titulo">Registrar ausência</h2>
        <p className="painel-subtitulo">Registre quem não compareceu à consulta agendada.</p>
      </div>

      <div className="campos-linha">
        <label className="campo">
          <span className="campo-label">ID da consulta</span>
          <input
            className="campo-controle"
            type="number"
            min="1"
            required
            value={consultaId}
            onChange={(evento) => setConsultaId(evento.target.value)}
            placeholder="Ex.: 45"
          />
        </label>

        <label className="campo">
          <span className="campo-label">Quem faltou?</span>
          <select
            className="campo-controle"
            required
            value={quemFaltou}
            onChange={(evento) => setQuemFaltou(evento.target.value)}
          >
            <option value="">Selecione</option>
            <option value="PACIENTE">Paciente</option>
            <option value="ALUNO">Aluno</option>
          </select>
        </label>
      </div>

      <label className="campo">
        <span className="campo-label">Observação</span>
        <textarea
          className="campo-controle campo-textarea"
          rows="3"
          maxLength="1000"
          value={justificativa}
          onChange={(evento) => setJustificativa(evento.target.value)}
          placeholder="Adicione uma observação sobre a ausência."
        />
      </label>

      <label className="campo-checkbox">
        <input
          type="checkbox"
          checked={justificada}
          onChange={(evento) => setJustificada(evento.target.checked)}
        />
        <span>Falta justificada</span>
      </label>

      <button className="btn-primario" type="submit" disabled={enviando || !consultaId || !quemFaltou}>
        {enviando ? 'Registrando...' : 'Registrar falta'}
      </button>
      <p className="painel-nota">A consulta precisa estar agendada ou confirmada para registrar a falta.</p>
    </form>
  )
}

export default PainelRegistroFalta
