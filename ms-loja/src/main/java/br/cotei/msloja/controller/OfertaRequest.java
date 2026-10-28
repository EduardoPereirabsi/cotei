package br.cotei.msloja.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OfertaRequest(
        @NotNull Long produtoId,
        @NotBlank String loja,
        @NotNull @PositiveOrZero BigDecimal preco,
        @PositiveOrZero BigDecimal frete,
        boolean disponivel,
        String url) {
}
