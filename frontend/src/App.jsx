import { useState } from "react";
import axios from "axios";
import Login from "./Login/Login.jsx";
import FilaEspera from "./FilaEspera.jsx";

function App() {
  const [usuario, setUsuario] = useState(null);

  async function sair() {
    try {
      await axios.post("/api/auth/logout", {}, { withCredentials: true });
    } catch (erro) {
      console.warn("Não foi possível invalidar a sessão no servidor.", erro);
    } finally {
      localStorage.removeItem("usuarioLogado");
      setUsuario(null);
    }
  }

  if (!usuario) return <Login onLogin={setUsuario} />;

  if (usuario.perfil === "SECRETARIA") {
    return <FilaEspera usuario={usuario} onLogout={sair} />;
  }

  return (
    <main style={{ minHeight: "100vh", padding: 32 }}>
      <h1>Bem-vindo, {usuario.nome}</h1>
      <p>Perfil: {usuario.perfil}</p>
      <p>A área deste perfil será integrada pela equipe.</p>
      <button type="button" onClick={sair}>Sair</button>
    </main>
  );
}

export default App;
