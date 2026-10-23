import { useState } from 'react'
import Login from './Login/Login.jsx'
import PainelAluno from './PainelAluno/PainelAluno.jsx'

function lerUsuarioSalvo() {
  try {
    return JSON.parse(localStorage.getItem('usuarioLogado'))
  } catch {
    return null
  }
}

function App() {
  const [usuario, setUsuario] = useState(lerUsuarioSalvo)

  function sair() {
    localStorage.removeItem('usuarioLogado')
    setUsuario(null)
  }

  if (!usuario) {
    return <Login onLogin={setUsuario}/>
  }

  if (usuario.perfil === 'ALUNO') {
    return <PainelAluno usuario={usuario} onSair={sair} />
  }

  return (
    <div style={{ padding: 32 }}>
      <p>O perfil {usuario.perfil} ainda não tem tela.</p>
      <button onClick={sair}>Sair</button>
    </div>
  )
}

export default App