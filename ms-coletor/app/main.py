"""ms-coletor: coleta ofertas das lojas parceiras e publica oferta.atualizada.

REST     -> consulta o catalogo no ms-produto para saber o que coletar.
RabbitMQ -> publica cada oferta no exchange fanout cotei.ofertas; ms-loja e
            ms-alerta recebem todos os eventos, cada um na sua fila.
"""
import asyncio
import logging
import os
import socket
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import APIRouter, FastAPI, HTTPException, status
from fastapi.responses import JSONResponse, RedirectResponse

from .fontes import FONTES, centavos
from .mensageria import Publicador, RabbitIndisponivel
from .modelos import ColetaRequest, ColetaResultado, OfertaManual
from .produto_client import ProdutoIndisponivel, buscar_produto, listar_produtos_ativos

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("ms-coletor")

PREFIXO = "/coletor"
RABBITMQ_URL = "amqp://{}:{}@{}:5672/".format(
    os.getenv("RABBITMQ_USER", "cotei"),
    os.getenv("RABBITMQ_PASSWORD", "cotei-lab"),
    os.getenv("RABBITMQ_HOST", "rabbitmq"),
)

publicador = Publicador(RABBITMQ_URL)


@asynccontextmanager
async def ciclo_de_vida(_app: FastAPI):
    # O HTTP sobe na hora; a conexao com o RabbitMQ e feita em segundo plano, com retentativas.
    conexao = asyncio.create_task(publicador.conectar_com_retentativa())
    yield
    conexao.cancel()
    await publicador.fechar()


app = FastAPI(
    title="ms-coletor",
    version="0.1.0",
    description="Coleta ofertas das lojas parceiras e publica oferta.atualizada (Python, FastAPI, RabbitMQ).",
    docs_url=f"{PREFIXO}/docs",
    openapi_url=f"{PREFIXO}/openapi.json",
    redoc_url=None,
    lifespan=ciclo_de_vida,
)
router = APIRouter(prefix=PREFIXO)


@router.get("", include_in_schema=False)
def raiz():
    return RedirectResponse(f"{PREFIXO}/docs")


@router.get("/health", summary="Readiness: 200 so com o RabbitMQ conectado")
def health():
    if publicador.pronto:
        return {"status": "ok"}
    return JSONResponse(status_code=503, content={"status": "rabbitmq indisponivel"})


@router.get("/instance", summary="Nome do pod que respondeu")
def instance():
    return {"pod": socket.gethostname()}


@router.get("/fontes", summary="Lojas parceiras configuradas")
def fontes():
    return [{"loja": f.nome, "descricao": f.descricao()} for f in FONTES]


@router.post("/coletas", status_code=status.HTTP_202_ACCEPTED, response_model=ColetaResultado,
             summary="Coleta as ofertas nas lojas e publica um evento por oferta")
async def coletar(corpo: ColetaRequest | None = None) -> ColetaResultado:
    try:
        if corpo is None or not corpo.produtoIds:
            produtos = await listar_produtos_ativos()
            ignorados: list[int] = []
        else:
            encontrados = [(pid, await buscar_produto(pid)) for pid in corpo.produtoIds]
            produtos = [p for _, p in encontrados if p is not None]
            ignorados = [pid for pid, p in encontrados if p is None]
    except ProdutoIndisponivel:
        raise HTTPException(status_code=503, detail="Nao foi possivel consultar o catalogo no ms-produto")

    publicadas = 0
    try:
        for produto in produtos:
            for fonte in FONTES:
                oferta = fonte.coletar(produto)
                await publicador.publicar(oferta.como_evento())
                publicadas += 1
                log.info("[COLETA] publicada: produto=%d loja=%s preco=%s", produto.id, oferta.loja, oferta.preco)
    except RabbitIndisponivel:
        raise HTTPException(status_code=503, detail=f"RabbitMQ indisponivel ({publicadas} ofertas publicadas antes da falha)")

    return ColetaResultado(produtosColetados=len(produtos), ofertasPublicadas=publicadas, produtosIgnorados=ignorados)


@router.post("/ofertas", status_code=status.HTTP_202_ACCEPTED,
             summary="Publica uma oferta informada manualmente (sem passar pelas fontes)")
async def publicar_oferta(oferta: OfertaManual) -> dict:
    evento = {
        "produtoId": oferta.produtoId,
        "loja": oferta.loja,
        "preco": float(centavos(oferta.preco)),
        "frete": float(centavos(oferta.frete)),
        "disponivel": oferta.disponivel,
        "url": oferta.url,
        "coletadoEm": datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
    }
    try:
        await publicador.publicar(evento)
    except RabbitIndisponivel:
        raise HTTPException(status_code=503, detail="RabbitMQ indisponivel")
    log.info("[COLETA] publicada manualmente: produto=%d loja=%s preco=%s", oferta.produtoId, oferta.loja, oferta.preco)
    return {"publicado": evento}


app.include_router(router)
