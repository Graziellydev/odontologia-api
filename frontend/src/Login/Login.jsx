import { useState } from 'react';
import { ArrowLeft, ArrowRight, KeyRound, UserRound } from 'lucide-react';
import {
  entrar,
  solicitarRecuperacao,
  redefinirSenha,
  sairDaSessao,
} from '../services/autenticacaoService.js';
import clinicPhoto from '../assets/clinic-login.png';
import './Login.css';

const PERFIS = ['Aluno', 'Coordenador', 'Secretaria'];
const PERFIL_VALIDO = ['ALUNO', 'SECRETARIA', 'COORDENADOR'];
const PERFIL_API = { Aluno: 'ALUNO', Secretaria: 'SECRETARIA', Coordenador: 'COORDENADOR' };

function ApresentacaoClinica() {
  return (
    <section className="auth-presentation" aria-label="Apresentação da clínica">
      <a className="auth-brand" href="#inicio" aria-label="UniCatólica, clínica odontológica">
        <span>UniCatólica</span>
        <small>Clínica odontológica</small>
      </a>
      <div className="auth-story">
        <div className="auth-story-copy">
          <h1>O cuidado começa com organização.</h1>
          <p>Um espaço para aprender, acolher e acompanhar o atendimento odontológico da nossa comunidade.</p>
        </div>
        <img className="auth-clinic-image" src={clinicPhoto} alt="Atendimento odontológico em clínica-escola" />
        <div className="auth-highlights">
          <div><strong>Formação com propósito</strong><span>Aprendizado e cuidado contínuo.</span></div>
          <div><strong>Acolhimento com atenção</strong><span>Da triagem ao retorno.</span></div>
        </div>
      </div>
      <p className="auth-presentation-footer">Clínica acadêmica <span>•</span> UniCatólica <span>•</span> Ambiente demonstrativo</p>
    </section>
  );
}

function Feedback({ tipo = 'erro', titulo, children }) {
  return (
    <div className={`auth-feedback auth-feedback-${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'} aria-live="polite">
      <KeyRound size={20} aria-hidden="true" />
      <div><strong>{titulo}</strong>{children && <p>{children}</p>}</div>
    </div>
  );
}

function Login({ onLoginSuccess }) {
  const [tela, setTela] = useState('login');
  const [perfilReferencia, setPerfilReferencia] = useState('Aluno');
  const [identificador, setIdentificador] = useState('');
  const [senha, setSenha] = useState('');
  const [codigo, setCodigo] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [mostrarAvisoPerfil, setMostrarAvisoPerfil] = useState(false);

  function voltarLogin() {
    setTela('login');
    setErro('');
    setCodigo('');
    setNovaSenha('');
    setConfirmacao('');
  }

  async function handleLogin(event) {
    event.preventDefault();
    setErro('');
    setCarregando(true);
    try {
      const usuario = await entrar(identificador.trim(), senha);
      if (!PERFIL_VALIDO.includes(usuario?.perfil)) {
        setErro('Login ou senha inválidos');
        return;
      }
      if (PERFIL_API[perfilReferencia] !== usuario.perfil) {
        await sairDaSessao();
        setErro('Acesso bloqueado: esta seleção de perfil não corresponde ao acesso autorizado para a conta. Escolha o perfil correto e tente novamente.');
        return;
      }
      localStorage.setItem('usuarioLogado', JSON.stringify(usuario));
      onLoginSuccess(usuario);
    } catch (error) {
      setErro(error.status === 401 ? 'Login ou senha inválidos' : 'Não foi possível concluir o acesso. Tente novamente.');
    } finally {
      setCarregando(false);
    }
  }

  async function handleSolicitar(event) {
    event.preventDefault();
    setErro('');
    const valor = identificador.trim();
    if (!valor) {
      setErro('Informe seu e-mail institucional ou matrícula.');
      return;
    }
    setCarregando(true);
    try {
      await solicitarRecuperacao(valor);
      setIdentificador(valor);
      setTela('redefinir');
    } catch {
      setErro('Não foi possível enviar as instruções agora. Tente novamente.');
    } finally {
      setCarregando(false);
    }
  }

  async function handleRedefinir(event) {
    event.preventDefault();
    setErro('');
    if (!identificador.trim() || !/^\d{6}$/.test(codigo)) {
      setErro('Informe seu e-mail ou matrícula e o código de seis dígitos.');
      return;
    }
    if (novaSenha.length < 12) {
      setErro('A nova senha deve ter pelo menos 12 caracteres.');
      return;
    }
    if (novaSenha !== confirmacao) {
      setErro('As senhas não coincidem. Confira e tente novamente.');
      return;
    }
    setCarregando(true);
    try {
      await redefinirSenha(identificador.trim(), codigo, novaSenha);
      setTela('concluido');
      setSenha('');
    } catch (error) {
      setErro(error.status === 400 ? 'Código inválido ou expirado.' : 'Não foi possível redefinir a senha. Tente novamente.');
    } finally {
      setCarregando(false);
    }
  }

  const recuperacao = tela !== 'login';

  return (
    <main className="auth-page">
      <ApresentacaoClinica />
      <section className="auth-access" aria-label={recuperacao ? 'Recuperação de acesso' : 'Acesso institucional'}>
        <div className="auth-access-content">
          {tela === 'login' && <>
            <header className="auth-heading">
              <span className="auth-eyebrow">PORTAL DA CLÍNICA</span>
              <h2>Bem-vindo de volta</h2>
              <p>Entre com suas credenciais institucionais.</p>
            </header>
            <div className="auth-profile-reference">
              <span className="auth-field-label">Perfil de referência</span>
              <div className="auth-profile-tabs" role="tablist" aria-label="Perfil de referência">
                {PERFIS.map((perfil) => <button key={perfil} type="button" role="tab" aria-selected={perfilReferencia === perfil} className={perfilReferencia === perfil ? 'is-active' : ''} onClick={() => setPerfilReferencia(perfil)}>{perfil}</button>)}
              </div>
              <p className="auth-reference-note">Seleção ilustrativa. As permissões são definidas pelo vínculo da sua conta, não pela opção escolhida.</p>
            </div>
            <form className="auth-form" onSubmit={handleLogin}>
              <label className="auth-field">
                <span className="auth-field-label">E-mail institucional ou matrícula</span>
                <input autoComplete="username" value={identificador} onChange={(event) => setIdentificador(event.target.value)} placeholder="mariana.costa@aluno.unicatolica.edu.br" required />
              </label>
              <label className="auth-field">
                <span className="auth-field-label">Senha</span>
                <input type="password" autoComplete="current-password" value={senha} onChange={(event) => setSenha(event.target.value)} placeholder="••••••••••••" required />
              </label>
              <button className="auth-forgot-link" type="button" onClick={() => { setErro(''); setTela('recuperar'); }}>Esqueci minha senha?</button>
              <button className="auth-button auth-button-primary auth-login-button" type="submit" disabled={carregando}>
                <ArrowRight size={17} aria-hidden="true" />{carregando ? 'Entrando...' : 'Entrar no sistema'}
              </button>
            </form>
            {erro && <Feedback titulo={erro}>{erro === 'Login ou senha inválidos' ? 'Confira suas credenciais institucionais e tente novamente.' : null}</Feedback>}
            <div className="auth-permissions">
              <h3>Permissões de acesso</h3>
              <p>Acesso exclusivo para Aluno, Secretaria e Coordenador. Cada conta possui permissões próprias.</p>
              <button className="auth-button auth-button-outline auth-profile-help" type="button" onClick={() => setMostrarAvisoPerfil((atual) => !atual)}>
                <UserRound size={16} aria-hidden="true" />{mostrarAvisoPerfil ? 'Fechar aviso de perfil' : 'Ver bloqueio de perfil'}
              </button>
              {mostrarAvisoPerfil && <Feedback tipo="aviso" titulo="Acesso exclusivo">A seleção de perfil não altera as permissões. Sua conta será encaminhada somente à área autorizada pelo sistema.</Feedback>}
            </div>
          </>}

          {(tela === 'recuperar' || tela === 'redefinir') && <>
            <header className="auth-heading auth-recovery-heading">
              <span className="auth-eyebrow">PORTAL DA CLÍNICA</span>
              <h2>Recupere seu acesso</h2>
              <p>Recupere sua conta institucional de Aluno, Secretaria ou Coordenador.</p>
            </header>
            {tela === 'recuperar' ? <form className="auth-form auth-recovery-form" onSubmit={handleSolicitar}>
              <label className="auth-field">
                <span className="auth-field-label">E-mail usado</span>
                <input autoComplete="username" value={identificador} onChange={(event) => setIdentificador(event.target.value)} placeholder="Digite o e-mail ou matrícula" maxLength={150} required />
              </label>
              {erro && <p className="auth-inline-error" role="alert">{erro}</p>}
              <button className="auth-button auth-button-primary auth-full-button" type="submit" disabled={carregando}><ArrowRight size={17} aria-hidden="true" />{carregando ? 'Enviando...' : 'Enviar instruções'}</button>
              <button className="auth-button auth-button-return" type="button" onClick={voltarLogin}><ArrowLeft size={17} aria-hidden="true" />Voltar para acessos</button>
              <Feedback tipo="aviso" titulo="Acesso exclusivo!">Use a conta fornecida pela instituição. Não há cadastro público para os perfis administrativos.</Feedback>
            </form> : <form className="auth-form auth-recovery-form" onSubmit={handleRedefinir}>
              <p className="auth-neutral-success" role="status">Se os dados corresponderem a uma conta elegível, enviaremos instruções para o e-mail cadastrado. Confira sua caixa de entrada e informe o código recebido.</p>
              <label className="auth-field"><span className="auth-field-label">E-mail usado</span><input autoComplete="username" value={identificador} onChange={(event) => setIdentificador(event.target.value)} required /></label>
              <label className="auth-field"><span className="auth-field-label">Código de 6 dígitos</span><input inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} value={codigo} onChange={(event) => setCodigo(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="000000" required /></label>
              <label className="auth-field"><span className="auth-field-label">Nova senha</span><input type="password" autoComplete="new-password" minLength={12} maxLength={72} value={novaSenha} onChange={(event) => setNovaSenha(event.target.value)} required /><small className="auth-field-hint">Use pelo menos 12 caracteres.</small></label>
              <label className="auth-field"><span className="auth-field-label">Confirmar nova senha</span><input type="password" autoComplete="new-password" minLength={12} maxLength={72} value={confirmacao} onChange={(event) => setConfirmacao(event.target.value)} required /></label>
              {erro && <p className="auth-inline-error" role="alert">{erro}</p>}
              <button className="auth-button auth-button-primary auth-full-button" type="submit" disabled={carregando}><ArrowRight size={17} aria-hidden="true" />{carregando ? 'Redefinindo...' : 'Redefinir senha'}</button>
              <button className="auth-button auth-button-return" type="button" onClick={voltarLogin}><ArrowLeft size={17} aria-hidden="true" />Voltar para acessos</button>
            </form>}
          </>}

          {tela === 'concluido' && <div className="auth-completion">
            <header className="auth-heading auth-recovery-heading"><span className="auth-eyebrow">PORTAL DA CLÍNICA</span><h2>Acesso recuperado</h2><p>Sua senha foi redefinida. Entre com sua nova senha.</p></header>
            <Feedback tipo="sucesso" titulo="Senha redefinida com sucesso." />
            <button className="auth-button auth-button-primary auth-full-button" type="button" onClick={voltarLogin}>Voltar para acessos</button>
          </div>}
        </div>
      </section>
    </main>
  );
}

export default Login;
