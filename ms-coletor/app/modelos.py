from decimal import Decimal

from pydantic import BaseModel, Field, PositiveInt


class ColetaRequest(BaseModel):
    """Sem produtoIds, coleta todos os produtos ativos do catalogo."""
    produtoIds: list[PositiveInt] | None = Field(default=None, examples=[[1]])


class ColetaResultado(BaseModel):
    produtosColetados: int
    ofertasPublicadas: int
    produtosIgnorados: list[int]


class OfertaManual(BaseModel):
    """Oferta publicada diretamente, sem passar pelas fontes (util para demonstracoes)."""
    produtoId: PositiveInt = Field(examples=[1])
    loja: str = Field(min_length=1, max_length=100, examples=["Loja Alfa"])
    preco: Decimal = Field(ge=0, max_digits=12, decimal_places=2, examples=[3499.90])
    frete: Decimal = Field(default=Decimal("0"), ge=0, max_digits=12, decimal_places=2, examples=[0])
    disponivel: bool = True
    url: str | None = Field(default=None, examples=["https://alfa.exemplo/p/1"])
