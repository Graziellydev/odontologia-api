import { useEffect, useMemo, useState } from 'react';
import { Download, ShieldCheck } from 'lucide-react';
import {
  buscarRelatorioDemandaClinica,
  buscarRelatorioDemografico,
  exportarRelatorioCoordenacao,
} from '../services/relatoriosCoordenacaoService.js';
import './RelatoriosCoordenacao.css';

const numero = new Intl.NumberFormat('pt-BR');
const percentual = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
const PALETA = ['#f97316', '#0f9f91', '#5684c7', '#a879c3', '#e7ad48', '#738493'];

function erroUsuario(error) {
  if (error.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.';
  if (error.status === 403) return 'Seu perfil não tem permissão para consultar estes relatórios.';
  if (error.status === 400) return error.message;
  return 'Não foi possível carregar o relatório. Verifique a conexão e tente novamente.';
}

function baixar(blob, nome) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function Donut({ itens }) {
  const total = itens.reduce((sum, item) => sum + (item.quantidade || 0), 0);
  let cursor = 0;
  const stops = itens.map((item, index) => {
    const start = cursor;
    cursor += total ? item.quantidade / total * 100 : 0;
    return `${PALETA[index % PALETA.length]} ${start}% ${cursor}%`;
  });
  return (
    <div className="relatorio-donut-layout">
      <div className="relatorio-donut" style={{ background: stops.length ? `conic-gradient(${stops.join(',')})` : '#e9eef1' }} role="img" aria-label={`Distribuição territorial de ${numero.format(total)} pacientes`}>
        <div className="relatorio-donut-hole"><strong>{numero.format(total)}</strong><span>pacientes</span></div>
      </div>
      <ul className="relatorio-legend">
        {itens.map((item, index) => <li key={item.categoria}><i style={{ background: PALETA[index % PALETA.length] }} /><span>{item.categoria}</span><strong>{percentual.format(item.percentual)}%</strong></li>)}
        {!itens.length && <li className="relatorio-muted">Sem dados territoriais.</li>}
      </ul>
    </div>
  );
}

function Filtros({ filtros, setFiltros, semestres, especialidades, regioes }) {
  const change = (key) => (event) => setFiltros((old) => ({ ...old, [key]: event.target.value }));
  return (
    <section className="relatorio-filters" aria-label="Filtros do relatório">
      <label><span>Semestre</span><select value={filtros.semestre} onChange={change('semestre')}><option value="">Todos os períodos</option>{semestres.map((s) => <option key={s} value={s}>{s}</option>)}</select></label>
      <label><span>Especialidade</span><select value={filtros.especialidade} onChange={change('especialidade')}><option value="">Todas as especialidades</option>{especialidades.map((s) => <option key={s} value={s}>{s}</option>)}</select></label>
      <label><span>Região</span><select value={filtros.regiao} onChange={change('regiao')}><option value="TODOS">Todas as regiões</option>{regioes.map((s) => <option key={s} value={s}>{s}</option>)}</select></label>
    </section>
  );
}

function GraficoFaixas({ itens }) {
  const max = Math.max(1, ...itens.map((item) => item.quantidade || 0));
  return <section className="relatorio-card" aria-labelledby="faixas-title"><div className="relatorio-card-heading"><div><h2 id="faixas-title">Distribuição por faixa etária</h2><p>Pacientes únicos por idade</p></div><span className="relatorio-chart-mark">01</span></div>
    {itens.length ? <ul className="relatorio-bars">{itens.map((item, index) => <li key={item.categoria}><div className="relatorio-bar-label"><span>{item.categoria}</span><strong>{numero.format(item.quantidade)}</strong></div><div className="relatorio-bar-track"><span style={{ width: `${item.quantidade / max * 100}%`, background: PALETA[index % PALETA.length] }} /></div><small>{percentual.format(item.percentual)}% da base</small></li>)}</ul> : <p className="relatorio-empty">Nenhum dado etário disponível para os filtros selecionados.</p>}
  </section>;
}

function ResumoTerritorial({ itens }) {
  return <section className="relatorio-card relatorio-territorial" aria-labelledby="territorial-title"><div className="relatorio-card-heading"><div><h2 id="territorial-title">Resumo territorial</h2><p>Origem dos pacientes no recorte selecionado</p></div></div>
    {itens.length ? <div className="relatorio-table-scroll"><table><thead><tr><th scope="col">Bairro / origem</th><th scope="col">Pacientes únicos</th><th scope="col">% da base</th></tr></thead><tbody>{itens.map((item) => <tr key={item.categoria}><th scope="row">{item.categoria}</th><td>{numero.format(item.quantidade)}</td><td>{percentual.format(item.percentual)}%</td></tr>)}</tbody></table></div> : <p className="relatorio-empty">Sem origens territoriais neste recorte.</p>}
  </section>;
}

export default function RelatoriosCoordenacao({ pagina, semestres = [] }) {
  const demografico = pagina === 'demografico';
  const [filtros, setFiltros] = useState({ semestre: '', especialidade: '', regiao: 'TODOS' });
  const [relatorio, setRelatorio] = useState(null);
  // Valores iniciais do catálogo clínico da migration V10; o backend ainda não expõe endpoint de catálogo.
  const opcoes = { especialidades: ['Cirurgia', 'Dentística', 'Endodontia', 'Periodontia'], regioes: ['Quixadá'] };
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [erroExportacao, setErroExportacao] = useState('');
  const [exportando, setExportando] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setCarregando(true); setErro('');
    const buscar = demografico ? buscarRelatorioDemografico : buscarRelatorioDemandaClinica;
    buscar(filtros, { signal: controller.signal }).then((data) => setRelatorio(data))
      .catch((error) => { if (error.name !== 'AbortError') setErro(erroUsuario(error)); })
      .finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => controller.abort();
  }, [demografico, filtros]);

  const itensDemanda = useMemo(() => relatorio?.porProcedimento || [], [relatorio]);
  const totalDemanda = relatorio?.totalItensTriagem ?? 0;
  const titulo = demografico ? 'Perfil demográfico' : 'Demanda clínica';

  async function exportar() {
    setExportando(true); setErroExportacao('');
    try {
      const blob = await exportarRelatorioCoordenacao(demografico ? 'demografico' : 'demanda-clinica', filtros, 'CSV');
      baixar(blob, `relatorio-${demografico ? 'demografico' : 'demanda-clinica'}.csv`);
    } catch (error) { setErroExportacao(erroUsuario(error)); }
    finally { setExportando(false); }
  }

  return <div className="relatorios-page">
    <div className="relatorios-heading"><div><p className="coordenador-eyebrow">COORDENAÇÃO · RELATÓRIOS</p><h1>{titulo}</h1><p className="relatorios-subtitle">{demografico ? 'Quem a clínica acolhe: uma visão agregada e protegida da população atendida.' : 'Acompanhe os procedimentos identificados na triagem e os registrados em consultas.'}</p></div><button type="button" className="relatorios-export" onClick={exportar} disabled={exportando || carregando}><Download size={17} aria-hidden="true" />{exportando ? 'Preparando arquivo…' : 'Exportar dados agregados'}</button></div>
    <Filtros filtros={filtros} setFiltros={setFiltros} semestres={semestres} especialidades={opcoes.especialidades} regioes={opcoes.regioes} />
    {erroExportacao && <p className="relatorios-error" role="alert">{erroExportacao}</p>}
    {carregando && <p className="relatorio-status" role="status">Carregando relatório…</p>}
    {!carregando && erro && <div className="relatorios-error" role="alert">{erro}<button type="button" onClick={() => setFiltros((old) => ({ ...old }))}>Tentar novamente</button></div>}
    {!carregando && !erro && demografico && relatorio && <>
      <section className="relatorio-kpis" aria-label="Indicadores principais"><article className="relatorio-kpi relatorio-kpi-primary"><span>Base de análise</span><strong>{numero.format(relatorio.baseAnalise?.totalPacientesUnicos || 0)}</strong><small>{relatorio.baseAnalise?.descricao || 'Pacientes únicos no recorte'}</small></article><article className="relatorio-kpi"><span>Faixa mais frequente</span><strong>{relatorio.faixaMaisFrequente?.categoria || '—'}</strong><small>{relatorio.faixaMaisFrequente ? `${numero.format(relatorio.faixaMaisFrequente.quantidade)} pacientes · ${percentual.format(relatorio.faixaMaisFrequente.percentual)}%` : 'Sem destaque disponível'}</small></article><article className="relatorio-kpi"><span>Bairro mais frequente</span><strong>{relatorio.origemMaisFrequente?.categoria || '—'}</strong><small>{relatorio.origemMaisFrequente ? `${numero.format(relatorio.origemMaisFrequente.quantidade)} pacientes · ${percentual.format(relatorio.origemMaisFrequente.percentual)}%` : 'Sem destaque disponível'}</small></article></section>
      <section className="relatorio-chart-grid"><GraficoFaixas itens={relatorio.distribuicaoFaixaEtaria || []} /><section className="relatorio-card" aria-labelledby="origem-title"><div className="relatorio-card-heading"><div><h2 id="origem-title">Bairros e origem</h2><p>Distribuição territorial da base</p></div><span className="relatorio-chart-mark">02</span></div><Donut itens={relatorio.distribuicaoTerritorial || []} /></section></section>
      <ResumoTerritorial itens={relatorio.distribuicaoTerritorial || []} />
      <div className="relatorio-privacy"><ShieldCheck size={21} aria-hidden="true" /><p><strong>Privacidade e LGPD</strong><span>Visualização agregada. Nomes, documentos, contatos e prontuários não são exibidos neste relatório.</span></p></div>
    </>}
    {!carregando && !erro && !demografico && relatorio && <>
      <section className="relatorio-kpis" aria-label="Indicadores principais"><article className="relatorio-kpi relatorio-kpi-primary"><span>Itens de triagem</span><strong>{numero.format(relatorio.totalItensTriagem || 0)}</strong><small>Demandas clínicas identificadas</small></article><article className="relatorio-kpi"><span>Consultas por procedimento</span><strong>{numero.format(relatorio.totalConsultas || 0)}</strong><small>Registros no recorte selecionado</small></article><article className="relatorio-kpi"><span>Procedimentos no ranking</span><strong>{numero.format(itensDemanda.length)}</strong><small>Categorias agregadas</small></article></section>
      <section className="relatorio-chart-grid"><section className="relatorio-card" aria-labelledby="demanda-procedimentos-title"><div className="relatorio-card-heading"><div><h2 id="demanda-procedimentos-title">Procedimentos identificados</h2><p>Itens registrados em triagens</p></div></div><ul className="relatorio-bars">{itensDemanda.map((item, index) => <li key={item.categoria}><div className="relatorio-bar-label"><span>{item.categoria}</span><strong>{numero.format(item.quantidade)}</strong></div><div className="relatorio-bar-track"><span style={{ width: `${Math.min(100, (item.quantidade / Math.max(1, totalDemanda)) * 100)}%`, background: PALETA[index % PALETA.length] }} /></div><small>{percentual.format(item.percentual)}% das triagens</small></li>)}{!itensDemanda.length && <li className="relatorio-muted">Sem procedimentos para os filtros selecionados.</li>}</ul></section><section className="relatorio-card"><div className="relatorio-card-heading"><div><h2>Registros por especialidade</h2><p>Distribuição disponível no resumo agregado</p></div></div><ul className="relatorio-demand-list">{(relatorio.porEspecialidade || []).map((item, index) => <li key={item.categoria}><i style={{ background: PALETA[index % PALETA.length] }} /><span>{item.categoria}</span><strong>{numero.format(item.quantidade)}</strong><small>{percentual.format(item.percentual)}%</small></li>)}{!(relatorio.porEspecialidade || []).length && <li className="relatorio-muted">Sem dados de especialidade.</li>}</ul></section></section>
      <p className="relatorio-demand-note">{relatorio.aviso}</p>
      <div className="relatorio-privacy"><ShieldCheck size={21} aria-hidden="true" /><p><strong>Privacidade e LGPD</strong><span>Relatório agregado; não exibe dados pessoais ou prontuários.</span></p></div>
    </>}
  </div>;
}
