import { useState } from "react";
import Login from "./Login/Login.jsx";
import CadastroPaciente from "./Pacientes/CadastroPaciente.jsx";
import AplicacaoAluno from "./Aluno/AplicacaoAluno.jsx";

function App() {
  const [usuario, setUsuario] = useState(() => {
    try {
      const salvo = localStorage.getItem("usuarioLogado");
      return salvo ? JSON.parse(salvo) : null;
    } catch {
      return null;
    }
  });

  function sair() {
    localStorage.removeItem("usuarioLogado");
    setUsuario(null);
  }

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
      <CadastroPaciente
        nomeUsuario={usuario.nome}
        onLogout={sair}
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