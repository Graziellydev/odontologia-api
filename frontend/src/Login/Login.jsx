import React, { useState } from 'react';
import { api } from '../api.js';
import './Login.css';

function Login({ aoEntrar }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mensagemErro, setMensagemErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setMensagemErro('');
    setCarregando(true);

    try {
      const usuario = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim(), senha }),
      });
      if (usuario.perfil !== 'ALUNO') {
        await api('/auth/logout', { method: 'POST' });
        setMensagemErro('Este painel é exclusivo de alunos.');
        return;
      }
      setSenha('');
      aoEntrar(usuario);
    } catch (error) {
      setMensagemErro(error.status === 401 ? 'E-mail ou senha incorretos.' : error.message);
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="login-container">
      {/* LADO ESQUERDO: Painel Institucional */}
      <div className="login-left-panel">
        <div className="login-overlay">
          <div>
            <h1 className="logo-left">
              Uni<span className="logo-orange">Católica</span>
            </h1>
            <p className="subtitle-left">Clínica Odontológica</p>
          </div>

          <div className="hero-container">
            <h2 className="hero-title">Sistema de Gerenciamento Clínico</h2>
            <p className="hero-subtitle">
              Gestão integrada de atendimentos, triagens e prontuários da clínica-escola.
            </p>
          </div>

          <p className="footer-credits">
            Desenvolvido pelo Curso de Ciência da Computação
          </p>
        </div>
      </div>

      {/* LADO DIREITO: Formulário */}
      <div className="login-right-panel">
        <div className="login-form-card">
          <div className="login-header">
            <h1 className="logo-right">
              Uni<span className="logo-orange">Católica</span>
            </h1>
            <span className="badge-clinic">Clínica Odontológica</span>
            <h2 className="welcome-title">Acesse a área do aluno</h2>
            <p className="welcome-subtitle">
              Informe suas credenciais para entrar no sistema
            </p>
          </div>

          {mensagemErro && (
            <div className="error-banner">
              ⚠️ {mensagemErro}
            </div>
          )}

          <form onSubmit={handleLogin} className="login-form">
            <div className="input-group">
              <label className="input-label">E-mail institucional</label>
              <input
                type="email"
                placeholder="seu.email@unicatolica.edu.br"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="input-field"
              />
            </div>

            <div className="input-group">
              <label className="input-label">Senha</label>
              <input
                type="password"
                placeholder="••••••••"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                required
                className="input-field"
              />
            </div>

            <button
              type="submit"
              disabled={carregando}
              className="login-button"
            >
              {carregando ? 'Entrando...' : 'Entrar no Sistema'}
            </button>
          </form>

          <footer className="form-footer">
            © {new Date().getFullYear()} UniCatólica — Acesso acadêmico
          </footer>
        </div>
      </div>
    </div>
  );
}

export default Login;
