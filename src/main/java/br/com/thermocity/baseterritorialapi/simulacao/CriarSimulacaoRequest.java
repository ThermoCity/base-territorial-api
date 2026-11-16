package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cenário de simulação (POST /simulacoes). Só icMaxPorDia e agentesPorUbs são obrigatórios.
 *
 * @param modeloId             modelo pronto de origem (GET /simulacoes/modelos), só para registro
 * @param refugios             TODOS (padrão), PUBLICOS ou NENHUM
 * @param contatosPorAgenteDia padrão 15
 * @param fatorK               estimativa entre 0,10 e 0,50; padrão 0,25
 * @param distrito             recorte opcional (ex.: "Presidente Prudente"); vazio = cidade toda
 */
public record CriarSimulacaoRequest(
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
        String nome,

        @Size(max = 40, message = "modeloId pode ter no máximo 40 caracteres")
        String modeloId,

        @NotEmpty(message = "Informe o índice de calor de pelo menos um dia")
        @Size(max = 15, message = "O cenário pode ter no máximo 15 dias")
        List<@NotNull(message = "Nenhum dia pode ficar sem índice de calor")
             @DecimalMin(value = "0", message = "O índice de calor deve ser maior ou igual a 0 °C")
             @DecimalMax(value = "70", message = "O índice de calor deve ser menor ou igual a 70 °C") Double> icMaxPorDia,

        LocalDate dataInicio,

        ModoRefugios refugios,

        @NotNull(message = "Informe o número de agentes por UBS")
        @Min(value = 1, message = "O número de agentes por UBS deve ser pelo menos 1")
        @Max(value = 50, message = "O número de agentes por UBS deve ser no máximo 50")
        Integer agentesPorUbs,

        @Min(value = 1, message = "Os contatos por agente por dia devem ser pelo menos 1")
        @Max(value = 100, message = "Os contatos por agente por dia devem ser no máximo 100")
        Integer contatosPorAgenteDia,

        @DecimalMin(value = "0.10", message = "O fator k deve estar entre 0,10 e 0,50")
        @DecimalMax(value = "0.50", message = "O fator k deve estar entre 0,10 e 0,50")
        BigDecimal fatorK,

        @Size(max = 255, message = "O distrito pode ter no máximo 255 caracteres")
        String distrito) {
}
