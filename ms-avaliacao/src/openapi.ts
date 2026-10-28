/**
 * Especificacao OpenAPI do ms-avaliacao.
 * O servidor e relativo ("/avaliacao"): o Swagger chama a API no mesmo host e porta
 * em que foi aberto, entao o "Try it out" funciona no Minikube e no EKS.
 */
const avaliacao = {
  type: "object",
  properties: {
    id: { type: "string", example: "6720b1c4e1a2f3a9b8c7d6e5" },
    produtoId: { type: "integer", example: 1 },
    autor: { type: "string", example: "Maria" },
    nota: { type: "integer", minimum: 1, maximum: 5, example: 5 },
    comentario: { type: "string", example: "Chegou rapido e bem embalado" },
    criadaEm: { type: "string", format: "date-time" },
  },
};

const novaAvaliacao = {
  type: "object",
  required: ["produtoId", "autor", "nota"],
  properties: {
    produtoId: { type: "integer", example: 1 },
    autor: { type: "string", example: "Maria" },
    nota: { type: "integer", minimum: 1, maximum: 5, example: 5 },
    comentario: { type: "string", example: "Chegou rapido e bem embalado" },
  },
};

const idPath = { name: "id", in: "path", required: true, schema: { type: "string" } };
const produtoIdPath = { name: "produtoId", in: "path", required: true, schema: { type: "integer" } };

export const openapi = {
  openapi: "3.0.3",
  info: {
    title: "ms-avaliacao",
    version: "0.1.0",
    description: "Avaliacoes de produtos do Cotei (TypeScript, Express, MongoDB)",
  },
  servers: [{ url: "/avaliacao" }],
  paths: {
    "/avaliacoes": {
      get: {
        summary: "Lista avaliacoes, opcionalmente de um produto",
        parameters: [{ name: "produtoId", in: "query", required: false, schema: { type: "integer" } }],
        responses: { "200": { description: "Lista de avaliacoes", content: { "application/json": { schema: { type: "array", items: avaliacao } } } } },
      },
      post: {
        summary: "Registra uma avaliacao (valida o produto no ms-produto)",
        requestBody: { required: true, content: { "application/json": { schema: novaAvaliacao } } },
        responses: {
          "201": { description: "Avaliacao criada", content: { "application/json": { schema: avaliacao } } },
          "400": { description: "Dados invalidos" },
          "422": { description: "Produto inexistente no catalogo" },
          "503": { description: "ms-produto indisponivel" },
        },
      },
    },
    "/avaliacoes/{id}": {
      get: {
        summary: "Busca uma avaliacao",
        parameters: [idPath],
        responses: { "200": { description: "Avaliacao", content: { "application/json": { schema: avaliacao } } }, "404": { description: "Nao encontrada" } },
      },
      delete: {
        summary: "Remove uma avaliacao",
        parameters: [idPath],
        responses: { "204": { description: "Removida" }, "404": { description: "Nao encontrada" } },
      },
    },
    "/produtos/{produtoId}/resumo": {
      get: {
        summary: "Nota media e total de avaliacoes do produto",
        parameters: [produtoIdPath],
        responses: { "200": { description: "Resumo calculado pelo MongoDB" } },
      },
    },
    "/instance": {
      get: { summary: "Nome do pod que respondeu", responses: { "200": { description: "Pod" } } },
    },
  },
};
