// Exclusivo de testes. Nunca importado pelo bundle de produção.
export function criarApiIsolada() {
  const pendente = {
    id: 1, alunoId: 10, alunoNome: "Aluno de teste A", alunoIdentificacao: "MAT-TESTE-A",
    turmaId: 20, turma: "TURMA-TESTE", disciplina: "Dentística", semestreAtual: 6,
    procedimentoId: 5, procedimento: "Restauração", dente: "11", classe: "I",
    dataDesejada: "2026-11-10", statusSolicitacao: "PENDENTE_APROVACAO",
    dataSolicitacao: "2026-11-02T10:00:00Z", observacaoSecretaria: "Observação original do aluno",
    respostaSecretaria: null, pacienteAlocadoId: null, pacienteAlocadoNome: null,
    filaEsperaId: null, dataAnalise: null
  };
  const demanda = {
    id: 101, pacienteId: 201, pacienteNome: "Paciente de teste A", procedimentoId: 5,
    procedimentoNome: "Restauração", dente: "11", classe: "I", nivelUrgencia: "ALTA",
    statusFila: "AGUARDANDO", diasEspera: 30, dataEntradaFila: "2026-10-03T10:00:00Z",
    prioridadeReagendamento: false, origem: "TRIAGEM"
  };
  const modelo = {
    perfil: "SECRETARIA", chamadas: [], falhas: [], antesDeResponder: null,
    solicitacoes: [
      pendente,
      { ...pendente, id: 2, alunoNome: "Aluno de teste B", alunoIdentificacao: "MAT-TESTE-B",
        statusSolicitacao: "RECUSADO", respostaSecretaria: "Recusa anterior de teste", dataAnalise: "2026-11-02T11:00:00Z" },
      { ...pendente, id: 3, statusSolicitacao: "APROVADO_E_ALOCADO", pacienteAlocadoId: 203,
        pacienteAlocadoNome: "Paciente de teste já vinculado", filaEsperaId: 103 },
      { ...pendente, id: 4, statusSolicitacao: "PENDENTE_FILA" }
    ],
    fila: [demanda, { ...demanda, id: 102, pacienteId: 202, pacienteNome: "Paciente de teste B", nivelUrgencia: "PNE" },
      { ...demanda, id: 103, pacienteId: 203, statusFila: "ALOCADO" },
      { ...demanda, id: 104, procedimentoId: 8, pacienteNome: "Paciente incompatível de teste" }],
    falhar(method, caminho, status, mensagem = "erro de teste") {
      this.falhas.push({ method, caminho, status, mensagem });
    },
    async responder(method, caminho, body) {
      this.chamadas.push({ method, caminho, body });
      await this.antesDeResponder?.(method, caminho);
      const url = new URL(caminho, "http://127.0.0.1");
      const rota = url.pathname.replace(/^\/api/, "");
      const indice = this.falhas.findIndex(f => f.method === method && f.caminho === rota);
      if (indice >= 0) {
        const falha = this.falhas.splice(indice, 1)[0];
        return { status: falha.status, data: { message: falha.mensagem } };
      }
      if (rota === "/auth/login" && method === "POST") {
        if (body?.email !== "secretaria@teste.local" || body?.senha !== "teste") return { status: 401, data: {} };
        this.perfil = "SECRETARIA";
        return { status: 200, data: { id: 7, nome: "Secretaria de teste", perfil: "SECRETARIA" } };
      }
      if (this.perfil !== "SECRETARIA") return { status: this.perfil ? 403 : 401, data: {} };
      if (rota === "/auth/me") return { status: 200, data: { id: 7, nome: "Secretaria de teste", perfil: "SECRETARIA" } };
      if (rota === "/solicitacoes-pacientes/administrativas" && method === "GET") return { status: 200, data: structuredClone(this.solicitacoes) };
      if (rota === "/fila-espera" && method === "GET") {
        const filtros = url.searchParams;
        const ordem = { ALTA: 1, PNE: 2, MEDIA: 3, BAIXA: 4 };
        const dados = this.fila.filter(e => e.statusFila === "AGUARDANDO"
          && (!filtros.has("procedimentoId") || String(e.procedimentoId) === filtros.get("procedimentoId"))
          && (!filtros.has("dente") || e.dente === filtros.get("dente"))
          && (!filtros.has("classe") || e.classe === filtros.get("classe")))
          .sort((a,b) => ordem[a.nivelUrgencia] - ordem[b.nivelUrgencia]
            || a.dataEntradaFila.localeCompare(b.dataEntradaFila)
            || Number(b.prioridadeReagendamento) - Number(a.prioridadeReagendamento));
        return { status: 200, data: structuredClone(dados) };
      }
      const decisao = rota.match(/^\/solicitacoes-pacientes\/(\d+)\/(recusar|aprovar)$/);
      if (decisao && method === "PATCH") {
        const pedido = this.solicitacoes.find(s => s.id === Number(decisao[1]));
        if (!pedido) return { status: 404, data: {} };
        if (pedido.statusSolicitacao !== "PENDENTE_APROVACAO") return { status: 409, data: {} };
        if (decisao[2] === "recusar") {
          const motivo = body?.observacaoSecretaria?.trim();
          if (!motivo || motivo.length > 5000) return { status: 400, data: {} };
          pedido.statusSolicitacao = "RECUSADO";
          pedido.respostaSecretaria = motivo;
        } else {
          if (!body?.filaEsperaId) return { status: 400, data: {} };
          const entrada = this.fila.find(e => e.id === body.filaEsperaId);
          if (!entrada) return { status: 404, data: {} };
          if (entrada.statusFila !== "AGUARDANDO" || entrada.procedimentoId !== pedido.procedimentoId
              || entrada.dente !== pedido.dente || (pedido.classe != null && entrada.classe !== pedido.classe)) {
            return { status: 409, data: {} };
          }
          entrada.statusFila = "ALOCADO";
          pedido.statusSolicitacao = "APROVADO_E_ALOCADO";
          pedido.filaEsperaId = entrada.id;
          pedido.pacienteAlocadoId = entrada.pacienteId;
          pedido.pacienteAlocadoNome = entrada.pacienteNome;
        }
        pedido.dataAnalise = "2026-11-02T12:00:00Z";
        pedido.secretariaAnalisadorId = 7;
        const { id, statusSolicitacao, observacaoSecretaria, respostaSecretaria, dataAnalise,
          secretariaAnalisadorId, filaEsperaId, pacienteAlocadoId } = pedido;
        return { status: 200, data: { id, statusSolicitacao, observacaoSecretaria, respostaSecretaria, dataAnalise,
          secretariaAnalisadorId, filaEsperaId, pacienteAlocadoId } };
      }
      return { status: 404, data: {} };
    }
  };
  return modelo;
}
