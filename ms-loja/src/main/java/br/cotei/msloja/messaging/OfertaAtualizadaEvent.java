package br.cotei.msloja.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/** Payload do evento oferta.atualizada, publicado pelo ms-coletor. */
public record OfertaAtualizadaEvent(
        Long produtoId,
        String loja,
        BigDecimal preco,
        BigDecimal frete,
        boolean disponivel,
        String url,
        Instant coletadoEm) {
}
