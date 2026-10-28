"""Fontes de ofertas: um adaptador por loja.

Decisao de projeto: sem scraping real. Cada loja implementa a interface
FonteDeOfertas; as fontes atuais SIMULAM a vitrine de uma loja. Trocar uma
fonte simulada por uma real e escrever um novo adaptador, sem alterar o
restante do servico.
"""
import random
import re
from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import ROUND_HALF_UP, Decimal


@dataclass(frozen=True)
class Produto:
    id: int
    nome: str
    categoria: str = ""


@dataclass(frozen=True)
class OfertaColetada:
    produto_id: int
    loja: str
    preco: Decimal
    frete: Decimal
    disponivel: bool
    url: str
    coletado_em: datetime

    def como_evento(self) -> dict:
        """Payload do evento oferta.atualizada (contrato em docs/CONVENCOES.md)."""
        return {
            "produtoId": self.produto_id,
            "loja": self.loja,
            "preco": float(self.preco),
            "frete": float(self.frete),
            "disponivel": self.disponivel,
            "url": self.url,
            "coletadoEm": self.coletado_em.isoformat(timespec="milliseconds").replace("+00:00", "Z"),
        }


def centavos(valor: float | Decimal) -> Decimal:
    return Decimal(str(valor)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


# Faixa de preco plausivel por categoria. O catalogo nao guarda preco, entao a
# simulacao parte de uma referencia estavel por produto dentro da faixa da categoria.
FAIXAS_POR_CATEGORIA = {
    "SMARTPHONE": (2500, 7000),
    "NOTEBOOK": (2800, 9000),
    "TV": (1800, 8000),
    "TABLET": (1500, 5000),
    "FONE": (150, 2500),
}
FAIXA_PADRAO = (100, 2000)


def preco_de_referencia(produto: Produto) -> float:
    """Preco base simulado, estavel para cada produto e coerente com a categoria."""
    minimo, maximo = FAIXAS_POR_CATEGORIA.get(produto.categoria.upper(), FAIXA_PADRAO)
    return random.Random(produto.id).uniform(minimo, maximo)


class FonteDeOfertas(ABC):
    nome: str
    dominio: str

    @abstractmethod
    def coletar(self, produto: Produto) -> OfertaColetada:
        """Consulta a vitrine da loja e devolve a oferta atual do produto."""

    def descricao(self) -> str:
        return (self.__doc__ or "").strip()

    def _url(self, produto: Produto) -> str:
        slug = re.sub(r"[^a-z0-9]+", "-", produto.nome.lower()).strip("-")
        return f"https://{self.dominio}/p/{produto.id}-{slug}"

    def _oferta(self, produto: Produto, preco: float, frete: Decimal, disponivel: bool = True) -> OfertaColetada:
        return OfertaColetada(produto.id, self.nome, centavos(preco), frete, disponivel,
                              self._url(produto), datetime.now(timezone.utc))


class LojaAlfa(FonteDeOfertas):
    """Preco na media do mercado, sempre com frete gratis."""
    nome = "Loja Alfa"
    dominio = "alfa.exemplo"

    def coletar(self, produto: Produto) -> OfertaColetada:
        preco = preco_de_referencia(produto) * random.uniform(0.95, 1.05)
        return self._oferta(produto, preco, Decimal("0.00"))


class LojaBeta(FonteDeOfertas):
    """Vitrine mais barata, mas cobra frete fixo de R$ 149,90."""
    nome = "Loja Beta"
    dominio = "beta.exemplo"

    def coletar(self, produto: Produto) -> OfertaColetada:
        preco = preco_de_referencia(produto) * random.uniform(0.88, 0.98)
        return self._oferta(produto, preco, Decimal("149.90"))


class LojaGama(FonteDeOfertas):
    """Frete gratis acima de R$ 3.000; as vezes fica sem estoque."""
    nome = "Loja Gama"
    dominio = "gama.exemplo"

    def coletar(self, produto: Produto) -> OfertaColetada:
        preco = preco_de_referencia(produto) * random.uniform(0.96, 1.08)
        frete = Decimal("0.00") if preco >= 3000 else Decimal("39.90")
        return self._oferta(produto, preco, frete, disponivel=random.random() < 0.85)


FONTES: list[FonteDeOfertas] = [LojaAlfa(), LojaBeta(), LojaGama()]
