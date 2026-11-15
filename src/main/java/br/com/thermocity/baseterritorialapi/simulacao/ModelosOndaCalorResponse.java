package br.com.thermocity.baseterritorialapi.simulacao;

import java.time.LocalDate;
import java.util.List;

/**
 * Modelos prontos de onda de calor para começar uma simulação.
 *
 * @param fonte          de onde vêm as ondas reais (estação e tabela)
 * @param versaoLimiar   limiares usados para calcular o nível de cada dia
 * @param maxHistoricoC  maior IC máximo diário da série; acima dele o dia é extrapolação
 */
public record ModelosOndaCalorResponse(String fonte, String versaoLimiar, Double maxHistoricoC, List<Modelo> modelos) {

    /**
     * @param tipo              REAL (como aconteceu) ou VARIACAO (mesma onda com acréscimo no IC)
     * @param inicioEpisodio    primeiro dia do episódio NC4/NC5 (sem os dias de entrada e saída)
     * @param diasExtrapolados  quantos dias passam do máximo histórico
     */
    public record Modelo(String id, String nome, String tipo, double acrescimoC,
                         LocalDate inicioEpisodio, LocalDate fimEpisodio, int diasEpisodio,
                         int nivelMaximo, double icMaxC, int diasExtrapolados, List<Dia> dias) {
    }

    /**
     * @param parteDoEpisodio false nos dias de entrada e saída que acompanham o episódio
     * @param extrapolacao    IC acima do maior valor já registrado na estação
     */
    public record Dia(int dia, LocalDate data, double icMaxC, int nivel, String nome,
                      boolean parteDoEpisodio, boolean extrapolacao) {
    }
}
