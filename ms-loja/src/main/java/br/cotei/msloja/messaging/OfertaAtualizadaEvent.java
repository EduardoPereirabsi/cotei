package br.cotei.msloja.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record OfertaAtualizadaEvent(
        Long produtoId,
        String loja,
        BigDecimal preco,
        BigDecimal frete,
        boolean disponivel,
        String url,
        Instant coletadoEm) {
}
