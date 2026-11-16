package br.com.thermocity.baseterritorialapi.buscaativa;

import java.util.ArrayList;
import java.util.List;

/**
 * Prioridade de cada pessoa na lista do dia, por pontos, com os motivos por escrito para o agente
 * entender por que alguém está na frente. Maior pontuação primeiro; empate: mais velho primeiro.
 *
 * <pre>
 * Idade 80+                         +3   | 70–79 +2 | 60–69 +1
 * Mora sozinho(a)                   +2
 * Mobilidade reduzida               +2
 * Condição crônica                  +1
 * Setor com nota de calor 5         +2   | nota 4 +1
 * Setor com IPVS 5 ou 6             +2   | IPVS 4 +1
 * Sem refúgio a até 800 m           +1
 * </pre>
 * Os pesos são uma proposta da equipe para o MVP (não vêm de um estudo); ficam aqui num lugar só
 * para o modo real e a simulação usarem a mesma regra.
 */
public final class RegraPrioridade {

    public static final int DISTANCIA_REFUGIO_M = 800;

    private RegraPrioridade() {
    }

    public record Prioridade(int pontos, List<String> motivos) {
    }

    public static Prioridade calcular(int idade, boolean moraSozinho, boolean mobilidadeReduzida,
                                      boolean condicaoCronica, Integer ipvsGrupo, Integer notaCalor,
                                      boolean semRefugioProximo) {
        int pontos = 0;
        List<String> motivos = new ArrayList<>();
        if (idade >= 80) {
            pontos += 3;
            motivos.add("80+ anos");
        } else if (idade >= 70) {
            pontos += 2;
            motivos.add("70+ anos");
        } else {
            pontos += 1;
            motivos.add("60–69 anos");
        }
        if (moraSozinho) {
            pontos += 2;
            motivos.add("mora sozinho(a)");
        }
        if (mobilidadeReduzida) {
            pontos += 2;
            motivos.add("mobilidade reduzida");
        }
        if (condicaoCronica) {
            pontos += 1;
            motivos.add("condição crônica");
        }
        if (notaCalor != null && notaCalor >= 5) {
            pontos += 2;
            motivos.add("setor entre os 20% mais quentes");
        } else if (notaCalor != null && notaCalor == 4) {
            pontos += 1;
            motivos.add("setor quente (nota 4)");
        }
        if (ipvsGrupo != null && ipvsGrupo >= 5) {
            pontos += 2;
            motivos.add("vulnerabilidade social alta (IPVS " + ipvsGrupo + ")");
        } else if (ipvsGrupo != null && ipvsGrupo == 4) {
            pontos += 1;
            motivos.add("vulnerabilidade social média (IPVS 4)");
        }
        if (semRefugioProximo) {
            pontos += 1;
            motivos.add("sem refúgio a até " + DISTANCIA_REFUGIO_M + " m");
        }
        return new Prioridade(pontos, List.copyOf(motivos));
    }
}
