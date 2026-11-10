package br.com.thermocity.baseterritorialapi.sintetico;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Quantidade de pessoas 60+ fictícias a gerar. A semente é opcional (mesma semente = mesmo resultado). */
public record GerarSinteticosRequest(
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        @Max(value = 50000, message = "Quantidade máxima é 50000")
        Integer quantidade,
        Long semente) {
}
