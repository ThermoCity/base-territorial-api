package br.com.thermocity.baseterritorialapi.simulacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um dia da série histórica: data e IC máximo (°C). */
public record DiaClima(LocalDate data, double icMaxC) {

    /** Usado pela consulta JPQL (a coluna é NUMERIC). */
    public DiaClima(LocalDate data, BigDecimal icMaxC) {
        this(data, icMaxC.doubleValue());
    }
}
