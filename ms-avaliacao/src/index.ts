import express from "express";
import mongoose from "mongoose";
import swaggerUi from "swagger-ui-express";
import { conectarComRetentativa } from "./db";
import { openapi } from "./openapi";
import { router } from "./routes";

const PORT = Number(process.env.PORT ?? 8080);
const PREFIXO = "/avaliacao";
const MONGO_URL = process.env.MONGO_URL ?? "mongodb://mongo-avaliacao:27017/avaliacaodb";

const app = express();
app.use(express.json());

// Tudo sob o prefixo do servico: o Ingress encaminha /avaliacao/... sem reescrever o caminho.
app.use(PREFIXO, router);
app.get(`${PREFIXO}/openapi.json`, (_req, res) => {
  res.json(openapi);
});
app.use(`${PREFIXO}/docs`, swaggerUi.serve, swaggerUi.setup(openapi));
app.get(PREFIXO, (_req, res) => {
  res.redirect(`${PREFIXO}/docs`);
});

// JSON malformado vira 400; qualquer outra falha nao tratada vira 500.
app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  if (typeof err === "object" && err !== null && (err as { type?: string }).type === "entity.parse.failed") {
    res.status(400).json({ erro: "JSON invalido" });
    return;
  }
  console.error("[ERRO]", err);
  res.status(500).json({ erro: "erro interno" });
});

const server = app.listen(PORT, "0.0.0.0", () => {
  console.log(`ms-avaliacao ouvindo na porta ${PORT}, prefixo ${PREFIXO}`);
});
void conectarComRetentativa(MONGO_URL);

// Encerramento limpo quando o Kubernetes para o pod.
for (const sinal of ["SIGTERM", "SIGINT"] as const) {
  process.on(sinal, () => {
    server.close();
    void mongoose.disconnect().finally(() => process.exit(0));
  });
}
