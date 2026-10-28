import os from "os";
import { Router } from "express";
import { isValidObjectId } from "mongoose";
import { Avaliacao } from "./model";
import { mongoConectado } from "./db";
import { produtoExiste, ProdutoIndisponivelError } from "./produtoClient";

export const router = Router();

/** Readiness: so fica pronto quando a conexao com o MongoDB existe. */
router.get("/health", (_req, res) => {
  if (mongoConectado()) {
    res.json({ status: "ok" });
  } else {
    res.status(503).json({ status: "mongo indisponivel" });
  }
});

/** Nome do pod que respondeu: evidencia de balanceamento entre replicas. */
router.get("/instance", (_req, res) => {
  res.json({ pod: os.hostname() });
});

router.get("/avaliacoes", async (req, res) => {
  const filtro: Record<string, unknown> = {};
  if (req.query.produtoId !== undefined) {
    const produtoId = Number(req.query.produtoId);
    if (!Number.isInteger(produtoId) || produtoId <= 0) {
      res.status(400).json({ erro: "produtoId deve ser inteiro positivo" });
      return;
    }
    filtro.produtoId = produtoId;
  }
  res.json(await Avaliacao.find(filtro).sort({ criadaEm: -1 }));
});

router.get("/avaliacoes/:id", async (req, res) => {
  const avaliacao = isValidObjectId(req.params.id) ? await Avaliacao.findById(req.params.id) : null;
  if (!avaliacao) {
    res.status(404).json({ erro: "avaliacao nao encontrada" });
    return;
  }
  res.json(avaliacao);
});

router.post("/avaliacoes", async (req, res) => {
  const { produtoId, autor, nota, comentario } = req.body ?? {};

  const erros: string[] = [];
  if (!Number.isInteger(produtoId) || produtoId <= 0) erros.push("produtoId deve ser inteiro positivo");
  if (typeof autor !== "string" || autor.trim() === "") erros.push("autor e obrigatorio");
  if (!Number.isInteger(nota) || nota < 1 || nota > 5) erros.push("nota deve ser inteiro de 1 a 5");
  if (comentario !== undefined && typeof comentario !== "string") erros.push("comentario deve ser texto");
  if (erros.length > 0) {
    res.status(400).json({ erros });
    return;
  }

  try {
    if (!(await produtoExiste(produtoId))) {
      res.status(422).json({ erro: `Produto ${produtoId} nao existe no catalogo` });
      return;
    }
  } catch (e) {
    if (e instanceof ProdutoIndisponivelError) {
      res.status(503).json({ erro: "Nao foi possivel consultar o ms-produto" });
      return;
    }
    throw e;
  }

  const criada = await Avaliacao.create({ produtoId, autor, nota, comentario });
  res.status(201).json(criada);
});

router.delete("/avaliacoes/:id", async (req, res) => {
  const removida = isValidObjectId(req.params.id) ? await Avaliacao.findByIdAndDelete(req.params.id) : null;
  res.status(removida ? 204 : 404).end();
});

/** Nota media e total de avaliacoes, calculados pelo proprio MongoDB (aggregation pipeline). */
router.get("/produtos/:produtoId/resumo", async (req, res) => {
  const produtoId = Number(req.params.produtoId);
  if (!Number.isInteger(produtoId) || produtoId <= 0) {
    res.status(400).json({ erro: "produtoId deve ser inteiro positivo" });
    return;
  }
  const [resultado] = await Avaliacao.aggregate([
    { $match: { produtoId } },
    { $group: { _id: null, media: { $avg: "$nota" }, total: { $sum: 1 } } },
  ]);
  res.json({
    produtoId,
    notaMedia: resultado ? Math.round(resultado.media * 10) / 10 : null,
    totalAvaliacoes: resultado ? resultado.total : 0,
  });
});
