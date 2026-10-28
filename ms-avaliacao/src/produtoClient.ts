const BASE_URL = process.env.PRODUTO_URL ?? "http://ms-produto:8080/produto";

export class ProdutoIndisponivelError extends Error {}

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
