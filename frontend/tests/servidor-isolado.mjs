import { createServer } from "vite";
import react from "@vitejs/plugin-react";
import { criarApiIsolada } from "./rf010-fixtures.mjs";

let modelo = criarApiIsolada();
modelo.perfil = null;
const banner = `<div style="padding:8px 16px;background:#fff0c2;color:#483600;font:13px system-ui;position:relative;z-index:1000">
<strong>RF010 — AMBIENTE ISOLADO: dados em memória, sem Supabase.</strong>
Login: secretaria@teste.local / teste
<button id="rf010-reset">Reiniciar dados de teste</button>
<button id="rf010-conflito">Simular alocação da demanda 101 por outro usuário</button>
</div>`;
const vite = await createServer({
  configFile: false, envDir: false,
  define: { "import.meta.env.VITE_API_URL": JSON.stringify("") },
  server: { host: "127.0.0.1", port: 5174, strictPort: true, proxy: {},
    headers: { "Content-Security-Policy": "connect-src 'self'" } },
  plugins: [react(), {
    name: "rf010-isolado",
    transformIndexHtml(html) {
      return html.replace("<body>", "<body>" + banner + `<script type="module">
        document.querySelector("#rf010-reset").onclick = async () => {
          await fetch("/__rf010/reset", {method:"POST"}); localStorage.clear(); location.reload();
        };
        document.querySelector("#rf010-conflito").onclick = async () => {
          await fetch("/__rf010/conflito", {method:"POST"});
          alert("Somente no ambiente isolado: demanda 101 agora está alocada. Confirme a aprovação para verificar o conflito.");
        };
      </script>`);
    },
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const caminho = req.url || "/";
        if (!caminho.startsWith("/api/") && !caminho.startsWith("/__rf010/")) return next();
        res.setHeader("Content-Type", "application/json; charset=utf-8");
        try {
          let texto = "";
          for await (const parte of req) { texto += parte; if (texto.length > 32768) throw new Error("Limite"); }
          if (caminho === "/__rf010/reset" && req.method === "POST") {
            modelo = criarApiIsolada(); modelo.perfil = null;
            return res.end(JSON.stringify({ reiniciado: true }));
          }
          if (caminho === "/__rf010/conflito" && req.method === "POST") {
            modelo.fila.find(e => e.id === 101).statusFila = "ALOCADO";
            return res.end(JSON.stringify({ isolado: true }));
          }
          const resposta = await modelo.responder(req.method, caminho, texto ? JSON.parse(texto) : undefined);
          res.statusCode = resposta.status;
          res.end(JSON.stringify(resposta.data));
        } catch {
          res.statusCode = 400;
          res.end(JSON.stringify({ message: "Requisicao isolada invalida." }));
        }
      });
    }
  }]
});
await vite.listen();
console.log("RF010 isolada: http://127.0.0.1:5174");
console.log("API simulada em memoria. Nao inicie o backend. Credenciais: secretaria@teste.local / teste.");
