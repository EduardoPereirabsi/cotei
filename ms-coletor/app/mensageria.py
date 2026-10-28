"""Publicacao do evento oferta.atualizada no RabbitMQ (exchange fanout cotei.ofertas)."""
import asyncio
import json
import logging

import aio_pika
from aio_pika import DeliveryMode, ExchangeType, Message

EXCHANGE_OFERTAS = "cotei.ofertas"

log = logging.getLogger("ms-coletor")


class RabbitIndisponivel(Exception):
    pass


class Publicador:
    def __init__(self, url: str):
        self._url = url
        self._conexao: aio_pika.abc.AbstractRobustConnection | None = None
        self._exchange: aio_pika.abc.AbstractExchange | None = None

    @property
    def pronto(self) -> bool:
        return self._exchange is not None and self._conexao is not None and not self._conexao.is_closed

    async def conectar_com_retentativa(self, intervalo: float = 3.0) -> None:
        """Tenta conectar ate conseguir. A readiness probe so libera trafego depois."""
        tentativa = 0
        while True:
            tentativa += 1
            try:
                self._conexao = await aio_pika.connect_robust(self._url)
                # Canal com publisher confirms (padrao do aio-pika): cada publish
                # so retorna depois que o broker confirma o recebimento.
                canal = await self._conexao.channel()
                # Mesmos parametros declarados pelos consumidores (duravel, sem auto-delete).
                self._exchange = await canal.declare_exchange(EXCHANGE_OFERTAS, ExchangeType.FANOUT, durable=True)
                log.info("[RABBIT] conectado (tentativa %d)", tentativa)
                return
            except Exception:
                log.warning("[RABBIT] indisponivel (tentativa %d), nova tentativa em %.0fs", tentativa, intervalo)
                await asyncio.sleep(intervalo)

    async def publicar(self, evento: dict) -> None:
        if not self.pronto:
            raise RabbitIndisponivel()
        mensagem = Message(
            json.dumps(evento).encode("utf-8"),
            content_type="application/json",
            delivery_mode=DeliveryMode.PERSISTENT,
        )
        await self._exchange.publish(mensagem, routing_key="")

    async def fechar(self) -> None:
        if self._conexao is not None:
            await self._conexao.close()
