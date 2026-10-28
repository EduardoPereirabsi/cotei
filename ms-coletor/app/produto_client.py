"""Cliente do catalogo: e do ms-produto que vem a lista do que coletar."""
import os

import httpx

from .fontes import Produto

BASE_URL = os.getenv("PRODUTO_URL", "http://ms-produto:8080/produto")
TIMEOUT = httpx.Timeout(3.0, connect=2.0)


class ProdutoIndisponivel(Exception):
    pass


async def listar_produtos_ativos() -> list[Produto]:
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as cliente:
            resposta = await cliente.get(f"{BASE_URL}/produtos")
            resposta.raise_for_status()
    except httpx.HTTPError as e:
        raise ProdutoIndisponivel() from e
    return [Produto(p["id"], p["nome"], p.get("categoria") or "") for p in resposta.json() if p.get("ativo", True)]


async def buscar_produto(produto_id: int) -> Produto | None:
    """Produto do catalogo, ou None se o ms-produto responder 404."""
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as cliente:
            resposta = await cliente.get(f"{BASE_URL}/produtos/{produto_id}")
    except httpx.HTTPError as e:
        raise ProdutoIndisponivel() from e
    if resposta.status_code == 404:
        return None
    if resposta.status_code != 200:
        raise ProdutoIndisponivel()
    corpo = resposta.json()
    return Produto(corpo["id"], corpo["nome"], corpo.get("categoria") or "")
