import { useState } from "react";
import axios from "axios";
import "./CadastroPaciente.css";

const formularioInicial = {
  nomeCompleto: "",
  cpf: "",
  dataNascimento: "",
  telefone: "",
  telefoneAlternativo: "",
  email: "",
  cep: "",
  logradouro: "",
  numeroEndereco: "",
  complemento: "",
  bairro: "",
  cidade: "",
  moraProximo: false,
  aceitaChamadaUltimaHora: false,
  termoCompromissoAssinado: false,
  disponibilidadeHorario: "",
  observacao: "",
};

function CadastroPaciente({ nomeUsuario, onLogout }) {
  const [formulario, setFormulario] = useState(formularioInicial);
  const [carregando, setCarregando] = useState(false);
  const [mensagem, setMensagem] = useState(null);

  // Função para buscar o endereço automaticamente pelo CEP
  async function buscarCep(cepDigitado) {
    const cepLimpo = cepDigitado.replace(/\D/g, "");
    if (cepLimpo.length === 8) {
      try {
        const resposta = await axios.get(`https://viacep.com.br/ws/${cepLimpo}/json/`);
        if (!resposta.data.erro) {
          setFormulario((anterior) => ({
            ...anterior,
            logradouro: resposta.data.logradouro || "",
            bairro: resposta.data.bairro || "",
            cidade: resposta.data.localidade || "",
          }));
        }
      } catch (erro) {
        console.error("Erro ao buscar o CEP:", erro);
      }
    }
  }

  function atualizarCampo(e) {
    const { name, value, type, checked } = e.target;

    setFormulario((anterior) => {
      const novoFormulario = {
        ...anterior,
        [name]: type === "checkbox" ? checked : value,
      };

      // Se o campo alterado for o CEP, dispara a busca automática
      if (name === "cep") {
        buscarCep(value);
      }

      return novoFormulario;
    });
  }

  async function cadastrarPaciente(e) {
    e.preventDefault();
    setMensagem(null);
    setCarregando(true);

    const dados = {
      ...formulario,
      nomeCompleto: formulario.nomeCompleto.trim(),
      cpf: formulario.cpf.replace(/\D/g, ""),
    };

    try {
      const resposta = await axios.post(
        "http://localhost:8080/api/pacientes",
        dados
      );

      setMensagem({
        tipo: "sucesso",
        texto: `Paciente ${resposta.data.nomeCompleto} cadastrado com sucesso.`,
      });

      setFormulario({ ...formularioInicial });
    } catch (erro) {
      const detalhe = erro.response?.data;

      setMensagem({
        tipo: "erro",
        texto:
          typeof detalhe === "string"
            ? detalhe
            : "Não foi possível cadastrar o paciente. Verifique os dados e a conexão com a API.",
      });
    } finally {
      setCarregando(false);
    }
  }

  return (
    <main className="cadastro-pagina">
      <header className="cadastro-topo">
        <div>
          <h1>
            Uni<span>Católica</span>
          </h1>
          <p>Clínica Odontológica</p>
        </div>

        <div className="cadastro-usuario">
          <span>Olá, {nomeUsuario}</span>
          <button type="button" onClick={onLogout}>
            Sair
          </button>
        </div>
      </header>

      <section className="cadastro-conteudo">
        <div className="cadastro-titulo">
          <span className="cadastro-etiqueta">
            ÁREA DA SECRETARIA
          </span>

          <h2>Cadastro de pacientes</h2>

          <p>
            Preencha os dados para registrar um novo paciente
            na clínica.
          </p>
        </div>

        {mensagem && (
          <div
            className={`cadastro-mensagem ${mensagem.tipo}`}
            role="status"
          >
            {mensagem.texto}
          </div>
        )}

        <form
          className="cadastro-formulario"
          onSubmit={cadastrarPaciente}
        >
          <fieldset>
            <legend>Dados pessoais</legend>

            <div className="cadastro-grade">
              <div className="cadastro-campo campo-completo">
                <label htmlFor="nomeCompleto">
                  Nome completo *
                </label>
                <input
                  id="nomeCompleto"
                  name="nomeCompleto"
                  value={formulario.nomeCompleto}
                  onChange={atualizarCampo}
                  maxLength={150}
                  placeholder="Ex.: Maria da Silva Santos"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="cpf">CPF *</label>
                <input
                  id="cpf"
                  name="cpf"
                  value={formulario.cpf}
                  onChange={atualizarCampo}
                  inputMode="numeric"
                  maxLength={14}
                  placeholder="Ex.: 123.456.789-00"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="dataNascimento">
                  Data de nascimento *
                </label>
                <input
                  id="dataNascimento"
                  name="dataNascimento"
                  type="date"
                  value={formulario.dataNascimento}
                  onChange={atualizarCampo}
                  max={new Date().toLocaleDateString("en-CA")}
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="telefone">
                  Telefone principal *
                </label>
                <input
                  id="telefone"
                  name="telefone"
                  type="tel"
                  value={formulario.telefone}
                  onChange={atualizarCampo}
                  maxLength={20}
                  placeholder="Ex.: (88) 9 9999-9999"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="telefoneAlternativo">
                  Telefone alternativo
                </label>
                <input
                  id="telefoneAlternativo"
                  name="telefoneAlternativo"
                  type="tel"
                  value={formulario.telefoneAlternativo}
                  onChange={atualizarCampo}
                  maxLength={20}
                  placeholder="Ex.: (88) 3521-0000"
                />
              </div>

              <div className="cadastro-campo campo-completo">
                <label htmlFor="email">E-mail</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={formulario.email}
                  onChange={atualizarCampo}
                  maxLength={150}
                  placeholder="Ex.: maria.silva@email.com"
                />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Endereço</legend>

            <div className="cadastro-grade">
              <div className="cadastro-campo">
                <label htmlFor="cep">CEP *</label>
                <input
                  id="cep"
                  name="cep"
                  value={formulario.cep}
                  onChange={atualizarCampo}
                  maxLength={9}
                  placeholder="Ex.: 62500-000"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="logradouro">
                  Rua / Avenida *
                </label>
                <input
                  id="logradouro"
                  name="logradouro"
                  value={formulario.logradouro}
                  onChange={atualizarCampo}
                  maxLength={150}
                  placeholder="Ex.: Rua Coronel Araújo"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="numeroEndereco">Número *</label>
                <input
                  id="numeroEndereco"
                  name="numeroEndereco"
                  value={formulario.numeroEndereco}
                  onChange={atualizarCampo}
                  maxLength={20}
                  placeholder="Ex.: 145 ou S/N"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="complemento">Complemento</label>
                <input
                  id="complemento"
                  name="complemento"
                  value={formulario.complemento}
                  onChange={atualizarCampo}
                  maxLength={100}
                  placeholder="Ex.: Próximo à praça / Apto 201"
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="bairro">Bairro *</label>
                <input
                  id="bairro"
                  name="bairro"
                  value={formulario.bairro}
                  onChange={atualizarCampo}
                  maxLength={100}
                  placeholder="Ex.: Centro"
                  required
                />
              </div>

              <div className="cadastro-campo">
                <label htmlFor="cidade">Cidade *</label>
                <input
                  id="cidade"
                  name="cidade"
                  value={formulario.cidade}
                  onChange={atualizarCampo}
                  maxLength={100}
                  placeholder="Ex.: Quixadá"
                  required
                />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Informações complementares</legend>

            <div className="cadastro-opcoes">
              <label>
                <input
                  type="checkbox"
                  name="moraProximo"
                  checked={formulario.moraProximo}
                  onChange={atualizarCampo}
                />
                Mora próximo à faculdade
              </label>

              <label>
                <input
                  type="checkbox"
                  name="aceitaChamadaUltimaHora"
                  checked={formulario.aceitaChamadaUltimaHora}
                  onChange={atualizarCampo}
                />
                Aceita chamadas de última hora
              </label>
              <label>
                <input
                  type="checkbox"
                  name="termoCompromissoAssinado"
                  checked={formulario.termoCompromissoAssinado}
                  onChange={atualizarCampo}
                />
                Termo de compromisso assinado
              </label>
            </div>

            <div className="cadastro-campo">
              <label htmlFor="disponibilidadeHorario">
                Disponibilidade de horário *
              </label>
              <input
                id="disponibilidadeHorario"
                name="disponibilidadeHorario"
                value={formulario.disponibilidadeHorario}
                onChange={atualizarCampo}
                maxLength={100}
                placeholder="Ex.: Segundas e quartas no período da manhã"
                required
              />
            </div>

            <div className="cadastro-campo">
              <label htmlFor="observacao">Observações</label>
              <textarea
                id="observacao"
                name="observacao"
                value={formulario.observacao}
                onChange={atualizarCampo}
                rows={3}
                placeholder="Ex.: Paciente relata sensibilidade no dente molar"
              />
            </div>
          </fieldset>

          <p className="cadastro-nota">
            * Campos obrigatórios.
          </p>

          <div className="cadastro-acoes">
            <button
              type="button"
              className="cadastro-botao-secundario"
              onClick={() => {
                setFormulario({ ...formularioInicial });
                setMensagem(null);
              }}
              disabled={carregando}
            >
              Limpar formulário
            </button>

            <button
              type="submit"
              className="cadastro-botao-principal"
              disabled={carregando}
            >
              {carregando
                ? "Cadastrando..."
                : "Cadastrar paciente"}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}

export default CadastroPaciente;