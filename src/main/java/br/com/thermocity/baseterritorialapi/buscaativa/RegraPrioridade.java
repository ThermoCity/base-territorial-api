package br.com.thermocity.baseterritorialapi.buscaativa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Prioridade de cada pessoa na lista do dia: regra <b>v1</b> da API Java de cadastro (modo real,
 * PrioridadeService), repetida aqui para a simulação ordenar a fila exatamente como o modo real.
 *
 * <pre>
 * Mora sozinho(a)                  +3
 * Mobilidade reduzida              +2
 * Condição crônica                 +2
 * 70 anos ou mais                  +2
 * Nota de calor do setor (1 a 5)   nota − 1  (0 a 4)
 * Setor com IPVS 5–6 / IPVS 4      +2 / +1
 * Refúgio a mais de 800 m          +1   (sem refúgio considerado também conta)
 * </pre>
 * Máximo de 16 pontos. Empate: mais velho primeiro, depois o menor id. Cada critério atendido vira um
 * motivo, no mesmo texto e na mesma ordem da API de cadastro (ex.: "mora sozinho · 70 anos · setor nota 5").
 * Se os pesos mudarem lá (thermocity.prioridade.pesos.*), mude aqui também e suba a versão.
 */
public final class RegraPrioridade {

    public static final String VERSAO = "v1";
    public static final int DISTANCIA_REFUGIO_M = 800;

    private RegraPrioridade() {
    }

    public record Prioridade(int pontos, List<String> motivos) {
    }

    /**
     * @param distanciaRefugioM distância até o refúgio considerado mais próximo; nula quando não há refúgio
     */
    public static Prioridade calcular(int idade, boolean moraSozinho, boolean mobilidadeReduzida,
                                      boolean condicaoCronica, Integer ipvsGrupo, Integer notaCalor,
                                      Double distanciaRefugioM) {
        int pontos = 0;
        List<String> motivos = new ArrayList<>();
        if (moraSozinho) {
            pontos += 3;
            motivos.add("mora sozinho");
        }
        if (mobilidadeReduzida) {
            pontos += 2;
            motivos.add("mobilidade reduzida");
        }
        if (condicaoCronica) {
            pontos += 2;
            motivos.add("condição crônica");
        }
        if (idade >= 70) pontos += 2;
        motivos.add(idade + " anos");
        if (notaCalor != null && notaCalor >= 2) { // nota 1 vale 0 ponto: não vira motivo
            pontos += notaCalor - 1;
            motivos.add("setor nota " + notaCalor);
        }
        if (ipvsGrupo != null && ipvsGrupo >= 5) {
            pontos += 2;
            motivos.add("IPVS " + ipvsGrupo);
        } else if (ipvsGrupo != null && ipvsGrupo == 4) {
            pontos += 1;
            motivos.add("IPVS 4");
        }
        if (semRefugioProximo(distanciaRefugioM)) {
            pontos += 1;
            motivos.add(distanciaRefugioM == null ? "sem refúgio considerado" : "refúgio a " + km(distanciaRefugioM));
        }
        return new Prioridade(pontos, List.copyOf(motivos));
    }

    public static boolean semRefugioProximo(Double distanciaRefugioM) {
        return distanciaRefugioM == null || distanciaRefugioM > DISTANCIA_REFUGIO_M;
    }

    private static String km(double metros) {
        return BigDecimal.valueOf(metros / 1000).setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " km";
    }
}
