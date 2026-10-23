
import { useState } from "react";
import Login from "./Login/Login.jsx";
import CadastroPaciente from "./Pacientes/CadastroPaciente.jsx";

function App() {
  const [usuario, setUsuario] = useState(() => {
    localStorage.removeItem("usuarioLogado");
    return null;
  });

  function sair() {
    localStorage.removeItem("usuarioLogado");
    setUsuario(null);
  }

  if (!usuario) {
    return <Login onLoginSuccess={setUsuario} />;
  }

  if (usuario.perfil !== "SECRETARIA") {
    return (
      <main style={{ padding: "40px", fontFamily: "Arial" }}>
        <h1>Acesso à tela indisponível</h1>
        <p>
          A tela de cadastro de pacientes é destinada à secretaria.
        </p>
        <button onClick={sair}>Sair</button>
      </main>
    );
  }

  return (
    <CadastroPaciente
      nomeUsuario={usuario.nome}
      onLogout={sair}
    />
  );
}

export default App;