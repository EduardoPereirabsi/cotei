package br.cotei.msalerta.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AlertaRequest(
        @NotNull @Positive Long produtoId,
        @NotBlank @Email String email,
        @NotNull @Positive BigDecimal precoAlvo) {
}
