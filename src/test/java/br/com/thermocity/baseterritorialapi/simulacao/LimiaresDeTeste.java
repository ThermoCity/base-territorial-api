package br.com.thermocity.baseterritorialapi.simulacao;

import java.math.BigDecimal;
import java.util.List;

/** Os mesmos limiares carregados no banco (versão A707-2003-2026-v1). */
final class LimiaresDeTeste {

    static final String VERSAO = "A707-2003-2026-v1";

    private LimiaresDeTeste() {
    }

    static List<LimiarNivelCalor> todos() {
        return List.of(
                limiar(1, "Normal", null, 1, "IC máx < 35.0 °C", "Rotina."),
                limiar(2, "Atenção", "35.0", 1, "IC máx ≥ 35.0 °C por 1 dia(s) seguido(s) — P90", "Atenção."),
                limiar(3, "Alerta", "36.0", 1, "IC máx ≥ 36.0 °C por 1 dia(s) seguido(s) — P95", "Alerta."),
                limiar(3, "Alerta", "35.0", 3, "IC máx ≥ 35.0 °C por 3 dia(s) seguido(s) — P90", "Alerta."),
                limiar(4, "Alto risco", "37.0", 1, "IC máx ≥ 37.0 °C por 1 dia(s) seguido(s) — P98", "Alto risco."),
                limiar(4, "Alto risco", "36.0", 3, "IC máx ≥ 36.0 °C por 3 dia(s) seguido(s) — P95", "Alto risco."),
                limiar(5, "Emergência", "38.5", 1, "IC máx ≥ 38.5 °C por 1 dia(s) seguido(s) — P99,5", "Emergência."),
                limiar(5, "Emergência", "37.0", 3, "IC máx ≥ 37.0 °C por 3 dia(s) seguido(s) — P98", "Emergência."));
    }

    private static LimiarNivelCalor limiar(int nivel, String nome, String icMin, int dias, String regra, String acao) {
        return new LimiarNivelCalor(null, VERSAO, (short) nivel, nome, icMin == null ? null : new BigDecimal(icMin),
                (short) dias, regra, acao, 'S');
    }
}
