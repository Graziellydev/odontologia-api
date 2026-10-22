import { useState } from "react";
import Login from "./Login/Login.jsx";
import FilaEspera from "./FilaEspera.jsx";

function App() {
  const [usuario, setUsuario] = useState(null);

  if (!usuario) {
    return <Login onLogin={setUsuario} />;
  }

  if (usuario.perfil === "SECRETARIA") {
    return <FilaEspera />;
  }

  return (
    <main style={{ padding: "30px" }}>
      <h2>Bem-vindo, {usuario.nome}</h2>
      <p>Perfil: {usuario.perfil}</p>
      <p>A tela deste perfil será integrada pela equipe.</p>
      <button onClick={() => {
        localStorage.removeItem("usuarioLogado");
        setUsuario(null);
      }}>
        Sair
      </button>
    </main>
  );
}

export default App;