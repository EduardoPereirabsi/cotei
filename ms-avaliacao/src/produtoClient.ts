/**
 * Comunicacao sincrona (REST) com o ms-produto, fonte da verdade do catalogo.
 * No cluster a URL usa o nome do Service: http://ms-produto:8080/produto
 */
const BASE_URL = process.env.PRODUTO_URL ?? "http://ms-produto:8080/produto";

export class ProdutoIndisponivelError extends Error {}

/**
 * @returns true se o produto existe, false se o ms-produto respondeu 404
 * @throws ProdutoIndisponivelError se nao foi possivel consultar o ms-produto
 */
export async function produtoExiste(produtoId: number): Promise<boolean> {
  let resposta: Response;
  try {
    resposta = await fetch(`${BASE_URL}/produtos/${produtoId}`, { signal: AbortSignal.timeout(3000) });
  } catch {
    throw new ProdutoIndisponivelError("ms-produto nao respondeu");
  }
  if (resposta.status === 200) return true;
  if (resposta.status === 404) return false;
  throw new ProdutoIndisponivelError(`ms-produto respondeu ${resposta.status}`);
}
