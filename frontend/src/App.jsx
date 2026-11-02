import { useEffect, useState } from "react";
import Login from "./Login/Login.jsx";
import AplicacaoAluno from "./Aluno/AplicacaoAluno.jsx";
import FilaEspera from "./FilaEspera.jsx";
import PainelCoordenador from "./Coordenador/PainelCoordenador.jsx";
import { api } from "./api.js";

function possuiCoordenadorSalvo() {
  try {
    const usuarioSalvo = JSON.parse(window.localStorage.getItem("usuarioLogado") || "null");
    return usuarioSalvo?.perfil === "COORDENADOR";
  } catch {
    return false;
  }
}

function App() {
  const [usuario, setUsuario] = useState(null);
  const [estadoSessao, setEstadoSessao] = useState(() => (
    possuiCoordenadorSalvo() ? "verificando" : "anonimo"
  ));
  const [erroVerificacao, setErroVerificacao] = useState("");

  useEffect(() => {
    if (estadoSessao !== "verificando") return undefined;

    let ativo = true;

    async function restaurarSessaoCoordenador() {
      setErroVerificacao("");
      try {
        const usuarioAtual = await api("/auth/me");
        if (ativo) {
          setUsuario(usuarioAtual);
          setEstadoSessao("autenticado");
        }
      } catch (erro) {
        if (!ativo) return;

        if (erro.status === 401) {
          setUsuario(null);
          setEstadoSessao("anonimo");
          return;
        }

        setErroVerificacao(erro.message);
        setEstadoSessao("indisponivel");
      }
    }

    restaurarSessaoCoordenador();
    return () => { ativo = false; };
  }, [estadoSessao]);

  function sair() {
    localStorage.removeItem("usuarioLogado");
    setUsuario(null);
    setEstadoSessao("anonimo");
  }

  async function sairCoordenador() {
    await api("/auth/logout", { method: "POST" });
    localStorage.removeItem("usuarioLogado");
    setUsuario(null);
    setEstadoSessao("anonimo");
  }

  if (estadoSessao === "verificando") {
    return (
      <main className="session-state" aria-live="polite">
        <div className="session-state-content">
          <span className="session-spinner" aria-hidden="true" />
          <h1>Verificando sessão</h1>
          <p>Aguarde enquanto confirmamos seu acesso.</p>
        </div>
      </main>
    );
  }

  if (estadoSessao === "indisponivel") {
    return (
      <main className="session-state" aria-live="polite">
        <div className="session-state-content">
          <h1>Não foi possível verificar sua sessão</h1>
          <p role="alert">{erroVerificacao}</p>
          <button type="button" onClick={() => setEstadoSessao("verificando")}>
            Tentar novamente
          </button>
        </div>
      </main>
    );
  }

  // Tela de login
  if (!usuario) {
    return <Login onLoginSuccess={setUsuario} />;
  }

  // Área do aluno
  if (usuario.perfil === "ALUNO") {
    return (
      <AplicacaoAluno
        usuario={usuario}
        aoSair={sair}
      />
    );
  }

  // Área da secretaria
  if (usuario.perfil === "SECRETARIA") {
    return (
      <FilaEspera
        usuario={usuario}
        onLogout={sair}
      />
    );
  }

  if (usuario.perfil === "COORDENADOR") {
    return (
      <PainelCoordenador
        usuario={usuario}
        onLogout={sairCoordenador}
      />
    );
  }

  // Outros perfis
  return (
    <main style={{ padding: "40px", fontFamily: "Arial" }}>
      <h1>Acesso indisponível</h1>
      <p>Seu perfil não possui uma área disponível neste sistema.</p>
      <button onClick={sair}>Sair</button>
    </main>
  );
}

export default App;