package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Índice de calor máximo de cada dia do cenário, em °C.
 *
 * @param icMaxPorDia         um valor por dia do cenário (até 60 dias)
 * @param icMaxDiasAnteriores opcional: IC dos dias logo antes do cenário, para a regra dos 3 dias seguidos
 *                            já valer no começo da simulação (ex.: cenário que começa no meio de uma onda de calor)
 * @param dataInicio          opcional: data do primeiro dia, só para devolver a data de cada dia
 */
public record NiveisSimulacaoRequest(
        @NotEmpty(message = "Informe o IC máximo de pelo menos um dia")
        @Size(max = 60, message = "O cenário pode ter no máximo 60 dias")
        List<@NotNull(message = "Nenhum dia pode ficar sem IC")
             @DecimalMin(value = "0", message = "IC deve ser maior ou igual a 0 °C")
             @DecimalMax(value = "70", message = "IC deve ser menor ou igual a 70 °C") Double> icMaxPorDia,

        @Size(max = 10, message = "Informe no máximo 10 dias anteriores")
        List<@NotNull(message = "Nenhum dia anterior pode ficar sem IC")
             @DecimalMin(value = "0", message = "IC deve ser maior ou igual a 0 °C")
             @DecimalMax(value = "70", message = "IC deve ser menor ou igual a 70 °C") Double> icMaxDiasAnteriores,

        LocalDate dataInicio) {
}
