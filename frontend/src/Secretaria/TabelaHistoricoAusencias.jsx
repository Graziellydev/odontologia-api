function formatarData(data) {
  if (!data) return '—'
  const valor = new Date(`${data}T00:00:00`)
  return Number.isNaN(valor.getTime()) ? data : valor.toLocaleDateString('pt-BR')
}

function TabelaHistoricoAusencias({ faltas = [], aoSelecionar, carregando = false }) {
  return (
    <section className="historico-ausencias" aria-labelledby="titulo-historico">
      <div className="historico-cabecalho">
        <div>
          <h2 className="historico-titulo" id="titulo-historico">Histórico de ausências</h2>
          <p className="historico-subtitulo">Consultas com falta registradas no sistema.</p>
        </div>
        <span className="historico-contagem">{faltas.length} registros</span>
      </div>

      <div className="tabela-wrapper">
        <table className="tabela-ausencias">
          <thead>
            <tr>
              <th scope="col">Data</th>
              <th scope="col">Ausência</th>
              <th scope="col">Vínculo</th>
              <th scope="col">Situação</th>
              <th scope="col">Próximo passo</th>
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr><td className="tabela-mensagem" colSpan="5">Carregando histórico...</td></tr>
            )}
            {!carregando && faltas.length === 0 && (
              <tr><td className="tabela-mensagem" colSpan="5">Nenhuma ausência registrada.</td></tr>
            )}
            {!carregando && faltas.map((falta) => {
              const nomeVinculo = falta.vinculoPaciente || falta.vinculoAluno || '—'
              return (
                <tr key={falta.consultaId}>
                  <td className="celula-data">{formatarData(falta.data)}</td>
                  <td>{falta.ausencia || '—'}</td>
                  <td>{nomeVinculo}</td>
                  <td><span className="status-pendente">{falta.situacao || 'Pendente'}</span></td>
                  <td>
                    <button className="btn-link" type="button" onClick={() => aoSelecionar(falta)}>
                      {falta.proximoPasso || 'Reagendar'}
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="rodape-tabela">
        <span>Exibindo {faltas.length} {faltas.length === 1 ? 'registro' : 'registros'} de ausência</span>
        <span>Histórico atualizado pela secretaria</span>
      </div>
    </section>
  )
}

export default TabelaHistoricoAusencias
