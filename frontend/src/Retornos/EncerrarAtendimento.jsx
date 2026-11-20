import { useEffect, useState } from 'react'
import { buscarAtendimentosDeHoje, registrarEncerramento } from '../services/retornoService.js'
import './EncerrarAtendimento.css'

const TURNOS = { MANHA: 'Manhã', TARDE: 'Tarde' }

// O api() monta a mensagem como "HTTP 409 — texto do servidor"; aqui ficamos só com o texto
function mensagemDeErro(erro) {
  if (!erro.status) return erro.message
  if (erro.status === 401) return 'Sua sessão expirou. Faça login novamente.'
  const detalhe = erro.message.replace(/^HTTP \d+\s*(—\s*)?/, '').trim()
  return detalhe || 'Não foi possível concluir a operação. Tente novamente.'
}

function EncerrarAtendimento() {
  const [tentativa, setTentativa] = useState(0)
  const [lista, setLista] = useState({ chave: null, dados: [], erro: '' })
  const [selecionado, setSelecionado] = useState(null)
  const [tipo, setTipo] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erroEnvio, setErroEnvio] = useState('')
  const [sucesso, setSucesso] = useState('')

  useEffect(() => {
    let cancelado = false
    buscarAtendimentosDeHoje()
      .then((dados) => {
        if (!cancelado) {
          setLista({ chave: tentativa, dados: Array.isArray(dados) ? dados : [], erro: '' })
        }
      })
      .catch((erro) => {
        if (!cancelado) setLista({ chave: tentativa, dados: [], erro: mensagemDeErro(erro) })
      })
    return () => {
      cancelado = true
    }
  }, [tentativa])

  const carregando = lista.chave !== tentativa
  const atendimentos = carregando ? [] : lista.dados.filter((item) => item.status !== 'REALIZADO')

  function abrir(atendimento) {
    setSelecionado(atendimento)
    setTipo('')
    setJustificativa('')
    setErroEnvio('')
    setSucesso('')
  }

  async function confirmar(evento) {
    evento.preventDefault()
    setErroEnvio('')

    if (!tipo) {
      setErroEnvio('Informe se o tratamento foi concluído ou se o paciente precisa retornar.')
      return
    }
    const necessitaRetorno = tipo === 'retorno'
    if (necessitaRetorno && !justificativa.trim()) {
      setErroEnvio('A justificativa técnica é obrigatória para sugerir o retorno.')
      return
    }

    setEnviando(true)
    try {
      const resposta = await registrarEncerramento(
        selecionado.consultaId,
        necessitaRetorno,
        justificativa.trim()
      )
      setSucesso(resposta.mensagem)
      setSelecionado(null)
      setTentativa((t) => t + 1)
    } catch (erro) {
      setErroEnvio(mensagemDeErro(erro))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="retorno">
      <h1 className="retorno-titulo">Encerrar atendimento</h1>
      <p className="retorno-sub">
        Ao fim do turno, informe se o tratamento foi concluído ou se o paciente precisa retornar.
      </p>

      {sucesso && (
        <div className="retorno-sucesso" role="status">
          {sucesso}
        </div>
      )}

      {!carregando && lista.erro && (
        <div className="retorno-erro" role="alert">
          <span>⚠️ {lista.erro}</span>
          <button type="button" onClick={() => setTentativa((t) => t + 1)}>
            Tentar novamente
          </button>
        </div>
      )}

      {carregando && <p className="retorno-aviso">Carregando seus atendimentos...</p>}

      {!carregando && !lista.erro && atendimentos.length === 0 && (
        <div className="retorno-vazio">Você não tem atendimentos abertos hoje.</div>
      )}

      <ul className="retorno-lista">
        {atendimentos.map((a) => (
          <li
            key={a.consultaId}
            className={`retorno-item${selecionado?.consultaId === a.consultaId ? ' ativo' : ''}`}
          >
            <div>
              <strong>{a.paciente}</strong>
              <span className="retorno-detalhe">
                {a.procedimento}
                {a.dente ? ` • Dente ${a.dente}` : ''}
                {a.classe ? ` • Classe ${a.classe}` : ''}
                {` • ${TURNOS[a.turno] || a.turno}`}
              </span>
            </div>
            <button type="button" className="retorno-botao" onClick={() => abrir(a)}>
              Encerrar atendimento
            </button>
          </li>
        ))}
      </ul>

      {selecionado && (
        <form className="retorno-painel" onSubmit={confirmar}>
          <h2>Encerrar o atendimento de {selecionado.paciente}</h2>

          <label className="retorno-opcao">
            <input
              type="radio"
              name="tipo"
              checked={tipo === 'concluido'}
              onChange={() => setTipo('concluido')}
            />
            Tratamento concluído
          </label>
          <label className="retorno-opcao">
            <input
              type="radio"
              name="tipo"
              checked={tipo === 'retorno'}
              onChange={() => setTipo('retorno')}
            />
            Necessita retorno
          </label>

          {tipo === 'retorno' && (
            <label className="retorno-campo">
              Justificativa técnica *
              <textarea
                value={justificativa}
                onChange={(e) => setJustificativa(e.target.value)}
                rows={4}
                maxLength={1000}
                placeholder="Ex.: Canal instrumentado, falta a obturação."
              />
            </label>
          )}

          {erroEnvio && (
            <div className="retorno-erro" role="alert">
              ⚠️ {erroEnvio}
            </div>
          )}

          <div className="retorno-acoes">
            <button
              type="button"
              className="retorno-secundario"
              onClick={() => setSelecionado(null)}
              disabled={enviando}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="retorno-botao"
              disabled={enviando || !tipo || (tipo === 'retorno' && !justificativa.trim())}
            >
              {enviando ? 'Registrando...' : 'Confirmar'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}

export default EncerrarAtendimento
