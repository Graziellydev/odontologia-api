import React, { useEffect, useMemo, useState } from 'react';
import { BookOpen, ClipboardList, CheckCircle2, Info, LogOut, Send, RefreshCcw } from 'lucide-react';
import { api } from '../api.js';
import '../style.css';

const API = '/solicitacoes-pacientes';
const CATALOGO = '/catalogo-solicitacoes';

const novoFormulario = () => ({
  turmaId: '',
  especialidade: '',
  procedimentoId: '',
  dente: '',
  classe: '',
  dataDesejada: '',
  observacaoSecretaria: ''
});
const rotulosStatus = {
  PENDENTE_APROVACAO: 'Pendente de aprovação',
  PENDENTE_FILA: 'Aprovada — aguardando paciente',
  APROVADO_E_ALOCADO: 'Aprovada — paciente vinculado',
  RECUSADO: 'Recusada',
};
const dataBR = (valor) => {
  if (!valor) return '—';
  const data = new Date(valor.length === 10 ? `${valor}T12:00:00` : valor);
  return Number.isNaN(data.getTime()) ? '—' : data.toLocaleDateString('pt-BR');
};


function AplicacaoAluno({ usuario, aoSair }) {
  const [aba, setAba] = useState('solicitar');
  const [form, setForm] = useState(novoFormulario);
  const [turmas, setTurmas] = useState([]);
  const [procedimentos, setProcedimentos] = useState([]);
  const [solicitacoes, setSolicitacoes] = useState([]);
  const [erroCatalogo, setErroCatalogo] = useState('');
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [carregandoTurmas, setCarregandoTurmas] = useState(true);
  const [carregandoProcedimentos, setCarregandoProcedimentos] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [detalhe, setDetalhe] = useState(null);

  const turma = turmas.find(item => String(item.id) === String(form.turmaId));
  const procedimento = procedimentos.find(item => String(item.id) === String(form.procedimentoId));
  const especialidades = useMemo(() => [...new Set(procedimentos.map(p => p.especialidade).filter(Boolean))].sort(), [procedimentos]);
  const listaProcedimentos = procedimentos.filter(p => !form.especialidade || p.especialidade === form.especialidade);
  const classeObrigatoria = procedimento?.exigeClasse === true;

  async function atualizarSolicitacoes() {
    try {
      const dados = await api(`${API}/minhas`);
      setSolicitacoes(Array.isArray(dados) ? dados : []);
      setErro('');
    } catch (e) {
      setErro(`Falha ao carregar minhas solicitações: ${e.message}`);
    }
  }

  useEffect(() => {
    let ativo = true;
    setCarregandoTurmas(true);
    api(`${CATALOGO}/turmas`)
      .then(dados => { if (ativo) { setTurmas(Array.isArray(dados) ? dados : []); setErroCatalogo(''); } })
      .catch(e => { if (ativo) setErroCatalogo(`Falha ao carregar disciplinas: ${e.message}`); })
      .finally(() => { if (ativo) setCarregandoTurmas(false); });
    atualizarSolicitacoes();
    return () => { ativo = false; };
  }, [usuario.id]);

  useEffect(() => {
    if (!form.turmaId) { setProcedimentos([]); setCarregandoProcedimentos(false); return; }
    let ativo = true;
    setCarregandoProcedimentos(true);
    setErroCatalogo('');
    api(`${CATALOGO}/procedimentos?turmaId=${form.turmaId}`)
      .then(dados => { if (ativo) { setProcedimentos(Array.isArray(dados) ? dados : []); setErroCatalogo(''); } })
      .catch(e => { if (ativo) setErroCatalogo(`Falha ao carregar procedimentos: ${e.message}`); })
      .finally(() => { if (ativo) setCarregandoProcedimentos(false); });
    return () => { ativo = false; };
  }, [form.turmaId]);

  function alterarTurma(id) {
    setForm(anterior => ({ ...anterior, turmaId: id, especialidade: '', procedimentoId: '', classe: '' }));
    setProcedimentos([]);
  }
  function alterarEspecialidade(nome) {
    setForm(anterior => ({ ...anterior, especialidade: nome, procedimentoId: '', classe: '' }));
  }
  async function enviar(evento) {
    evento.preventDefault();
    setErro(''); setSucesso(''); setEnviando(true);
    try {
      if (!form.turmaId || !form.procedimentoId || !form.dente) throw new Error('Informe a turma, o procedimento e o dente.');
      if (classeObrigatoria && !form.classe) throw new Error('Este procedimento exige informar a classe.');
      await api(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          turmaId: Number(form.turmaId),
          procedimentoId: Number(form.procedimentoId),
          dente: form.dente.trim(),
          classe: form.classe || null,
          dataDesejada: form.dataDesejada || null,
          observacaoSecretaria: form.observacaoSecretaria.trim() || null,
        }),
      });
      setSucesso('Solicitação registrada! O status inicial é Pendente de aprovação.');
      setForm(novoFormulario());
      setAba('historico');
      await atualizarSolicitacoes();
    } catch (e) {
      setErro(`Não foi possível enviar: ${e.message}`);
    } finally {
      setEnviando(false);
    }
  }

  const nomeTurma = (id) => {
    const encontrada = turmas.find(t => String(t.id) === String(id));
    return encontrada ? `${encontrada.disciplina} — ${encontrada.codigoTurma || encontrada.codigoturma || 'Turma'}` : `Turma ${id}`;
  };
  const nomeProcedimento = (id) => {
    const encontrado = procedimentos.find(p => String(p.id) === String(id));
    return encontrado?.nome || `Procedimento ${id}`;
  };

  return <div className="shell">
    <aside>
      <div className="brand">UniCatólica<small>Clínica odontológica</small></div>
      <div className="nav-caption">ÁREA DO ALUNO</div>
      <nav>
        <button className={aba === 'solicitar' ? 'active' : ''} onClick={() => setAba('solicitar')}><BookOpen/> Solicitar paciente</button>
        <button className={aba === 'historico' ? 'active' : ''} onClick={() => { setAba('historico'); atualizarSolicitacoes(); }}><ClipboardList/> Minhas requisições</button>
      </nav>
      <div className="side-card"><b>Solicitação de paciente</b><p>Informe a necessidade clínica. A secretaria avaliará o pedido e cuidará da vinculação.</p></div>
      <div className="side-bottom">UniCatólica<br/>Uso acadêmico</div>
    </aside>
    <main>
      <header>
        <span>Clínica acadêmica / Aluno</span>
        <div className="header-right"><div className="avatar">{usuario.nome?.slice(0, 2).toUpperCase() || 'AL'}</div><b>{usuario.nome}</b><button className="logout" onClick={aoSair}><LogOut size={15}/> Sair</button></div>
      </header>
      <div className="content">
        <h1>{aba === 'solicitar' ? 'Solicitação de paciente' : 'Minhas requisições'}</h1>
        <p className="subtitle">{aba === 'solicitar' ? 'Selecione sua disciplina e descreva o procedimento necessário.' : 'Acompanhe o andamento das suas solicitações.'}</p>
        {erro && <div className="alert error" role="alert"><Info size={17}/>{erro}</div>}
        {sucesso && <div className="alert success" role="status"><CheckCircle2 size={17}/>{sucesso}</div>}
        {aba === 'solicitar' ? <div className="two-col">
          <section className="panel">
            <h2>Nova requisição</h2>
            {erroCatalogo && <div className="alert error" role="alert"><Info size={16}/>{erroCatalogo}</div>}
            <form onSubmit={enviar}>
              <label>Disciplina / turma *
                <select required disabled={carregandoTurmas} value={form.turmaId} onChange={e => alterarTurma(e.target.value)}>
                  <option value="">{carregandoTurmas ? 'Carregando disciplinas...' : 'Selecione sua disciplina'}</option>
                  {turmas.map(t => <option key={t.id} value={t.id}>{t.disciplina} — {t.codigoTurma || t.codigoturma || 'Turma'} ({t.semestre})</option>)}
                </select>
              </label>
              {!carregandoTurmas && !erroCatalogo && turmas.length === 0 && <p className="notice">Nenhuma turma ativa foi encontrada para este aluno.</p>}
              <div className="field-row">
                <label>Especialidade *
                  <select required disabled={!form.turmaId || carregandoProcedimentos} value={form.especialidade} onChange={e => alterarEspecialidade(e.target.value)}>
                    <option value="">{carregandoProcedimentos ? 'Carregando...' : 'Selecione a especialidade'}</option>
                    {especialidades.map(especialidade => <option key={especialidade} value={especialidade}>{especialidade}</option>)}
                  </select>
                </label>
                <label>Procedimento *
                  <select required disabled={!form.especialidade || carregandoProcedimentos} value={form.procedimentoId} onChange={e => setForm(f => ({ ...f, procedimentoId: e.target.value, classe: '' }))}>
                    <option value="">Selecione o procedimento</option>
                    {listaProcedimentos.map(p => <option key={p.id} value={p.id}>{p.nome}</option>)}
                  </select>
                </label>
              </div>
              {form.turmaId && !carregandoProcedimentos && !erroCatalogo && procedimentos.length === 0 && <p className="notice">Esta turma não possui procedimentos ativos vinculados à sua disciplina.</p>}
              <div className="field-row">
                <label>Dente (FDI) *
                  <input required maxLength={2} pattern="([1-4][1-8]|[5-8][1-5])" title="Informe o número FDI, como 36" value={form.dente} onChange={e => setForm(f => ({ ...f, dente: e.target.value }))} placeholder="Ex.: 36" />
                </label>
                <label>Classe {classeObrigatoria ? '*' : '(se aplicável)'}
                  <select required={classeObrigatoria} value={form.classe} onChange={e => setForm(f => ({ ...f, classe: e.target.value }))}>
                    <option value="">{classeObrigatoria ? 'Selecione a classe' : 'Não se aplica'}</option>
                    {['I', 'II', 'III', 'IV', 'V', 'VI'].map(classe => <option key={classe} value={classe}>{classe}</option>)}
                  </select>
                </label>
              </div>
              <label>Data desejada (opcional)<input type="date" min={new Date().toLocaleDateString('en-CA')} value={form.dataDesejada} onChange={e => setForm(f => ({ ...f, dataDesejada: e.target.value }))} /></label>
              <label>
                Observações para a secretaria (opcional)

                <textarea
                  value={form.observacaoSecretaria}
                  onChange={(e) =>
                    setForm((anterior) => ({
                      ...anterior,
                      observacaoSecretaria: e.target.value
                    }))
                  }
                  maxLength={1000}
                  rows={4}
                  placeholder="Ex.: Preciso de um paciente para a próxima aula prática."
                />
              </label>
              <button type="submit" className="primary" disabled={enviando || !form.procedimentoId}><Send size={15}/>{enviando ? 'Enviando...' : 'Solicitar paciente'}</button>
            </form>
          </section>
          <section className="panel peach">
            <h2>Como funciona?</h2>
            <h3>1. Escolha sua disciplina</h3><p>O sistema mostra apenas as turmas em que você está matriculada(o).</p>
            <h3>2. Informe o procedimento necessário</h3><p>Especialidade, procedimento, dente e classe são verificados antes de enviar.</p>
            <h3>3. Aguarde a secretaria</h3><p>Sua solicitação fica pendente de aprovação. Você pode acompanhar o andamento em “Minhas requisições”.</p>
            <hr/><p>O aluno não escolhe pacientes da fila nem realiza agendamentos por esta tela.</p>
          </section>
        </div> : <section className="listing"><div className="list-title"><h2>Solicitações enviadas</h2><button className="refresh" onClick={atualizarSolicitacoes}><RefreshCcw size={14}/> Atualizar</button></div>
          <div className="table-wrap"><table><thead><tr><th>Solicitação</th><th>Enviada em</th><th>Perfil</th><th>Status</th><th>Detalhes</th></tr></thead>
            <tbody>{solicitacoes.length ? solicitacoes.map(s => <tr key={s.id}><td>REQ-{String(s.id).padStart(3, '0')}</td><td>{dataBR(s.dataSolicitacao)}</td><td>{nomeTurma(s.turmaId)}<br/><span className="muted">{nomeProcedimento(s.procedimentoId)} • Dente {s.dente}</span></td><td><span className="status">{rotulosStatus[s.statusSolicitacao] || s.statusSolicitacao}</span></td><td><button className="link" onClick={() => setDetalhe(s)}>Ver requisição</button></td></tr>) : <tr><td colSpan={5} className="empty">Você ainda não enviou solicitações.</td></tr>}</tbody>
          </table><div className="table-foot">{solicitacoes.length} requisição(ões)</div></div>
        </section>}
        <footer>UniCatólica • Clínica odontológica • Área do aluno</footer>
      </div>
    </main>
    {detalhe && <div className="modal-back" onClick={() => setDetalhe(null)}><div className="modal" role="dialog" aria-modal="true" aria-label="Detalhes da solicitação" onClick={e => e.stopPropagation()}><button className="close" aria-label="Fechar" onClick={() => setDetalhe(null)}>×</button>
      <h2>Requisição REQ-{String(detalhe.id).padStart(3, '0')}</h2>
      <p><b>Status:</b> {rotulosStatus[detalhe.statusSolicitacao] || detalhe.statusSolicitacao}</p>
      <p><b>Turma:</b> {nomeTurma(detalhe.turmaId)}</p>
      <p><b>Procedimento:</b> {nomeProcedimento(detalhe.procedimentoId)}</p>
      <p><b>Dente:</b> {detalhe.dente}</p><p><b>Classe:</b> {detalhe.classe || 'Não informada'}</p>
      <p><b>Data desejada:</b> {dataBR(detalhe.dataDesejada)}</p>
      {detalhe.observacaoSecretaria && (
        <p>
          <b>Observações enviadas à secretaria:</b>{' '}
          {detalhe.observacaoSecretaria}
        </p>
      )}

      {detalhe.respostaSecretaria && (
        <p>
          <b>Resposta da secretaria:</b>{' '}
          {detalhe.respostaSecretaria}
        </p>
    )}
          </div></div>}
        </div>;
      }

export default AplicacaoAluno;
