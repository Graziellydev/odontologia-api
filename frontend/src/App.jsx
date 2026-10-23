import { useState } from "react";
import Login from "./Login/Login.jsx";
import CadastroPaciente from "./Pacientes/CadastroPaciente.jsx";
import PainelAluno from "./PainelAluno/PainelAluno.jsx";

// Mantém a função que lê o usuário salvo ao atualizar a página
function lerUsuarioSalvo() {
  try {
    return JSON.parse(localStorage.getItem("usuarioLogado"));
  } catch {
    return null;
  }
}

function App() {
  const [usuario, setUsuario] = useState(lerUsuarioSalvo);

  function sair() {
    localStorage.removeItem("usuarioLogado");
    setUsuario(null);
  }

  // Se não estiver logado, exibe a tela de Login
  if (!usuario) {
    // Note que unifiquei a propriedade de sucesso para usar a que estiver configurada no seu componente Login
    return <Login onLoginSuccess={setUsuario} onLogin={setUsuario} />;
  }

  // ROTEAMENTO POR PERFIL:

  // 1. Se for Aluno, mostra o Painel do Aluno
  if (usuario.perfil === "ALUNO") {
    return <PainelAluno usuario={usuario} onSair={sair} />;
  }

  // 2. Se for Secretária, mostra o Cadastro de Paciente
  if (usuario.perfil === "SECRETARIA") {
    return (
      <CadastroPaciente
        nomeUsuario={usuario.nome}
        onLogout={sair}
      />
    );
  }

  // 3. Caso seja qualquer outro perfil que ainda não tem tela própria
  return (
    <main style={{ padding: "40px", fontFamily: "Arial" }}>
      <h1>Acesso à tela indisponível</h1>
      <p>O perfil "{usuario.perfil}" ainda não possui uma tela definida.</p>
      <button onClick={sair}>Sair</button>
    </main>
  );
}

export default App;