import { useState } from "react";
import Login from "./Login/Login.jsx";
import FilaEspera from "./FilaEspera.jsx";

function App() {
  const [usuario, setUsuario] = useState(null);

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
        <p>Esta área é destinada à secretaria.</p>
        <button onClick={sair}>Sair</button>
      </main>
    );
  }

  return <FilaEspera usuario={usuario} onLogout={sair} />;
}

export default App;
