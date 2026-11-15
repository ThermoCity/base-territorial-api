package br.com.thermocity.baseterritorialapi.simulacao;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras NC1–NC5: um dia acima do limiar ou calor persistente por 3 dias seguidos. */
class NivelCalorCalculadoraTest {

    private static List<Integer> niveis(List<Double> ic, List<Double> anteriores) {
        return NivelCalorCalculadora.classificar(LimiaresDeTeste.todos(), ic, anteriores).stream()
                .map(NivelDoDia::nivel).toList();
    }

    private static List<Integer> niveis(Double... ic) {
        return niveis(Arrays.asList(ic), null);
    }

    @Test
    void umDiaDefineONivelPeloLimiarDeUmDia() {
        assertThat(niveis(34.6, 35.0, 36.0, 37.2, 38.5)).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void limiarExatoJaEntraNoNivel() {
        assertThat(niveis(34.99, 35.0)).containsExactly(1, 2);
    }

    @Test
    void tresDiasAcimaDe35SobemParaNc3NoTerceiroDia() {
        assertThat(niveis(35.4, 35.8, 35.2)).containsExactly(2, 2, 3);
    }

    @Test
    void tresDiasAcimaDe36SobemParaNc4() {
        assertThat(niveis(36.4, 36.1, 36.0)).containsExactly(3, 3, 4);
    }

    @Test
    void tresDiasAcimaDe37SobemParaNc5() {
        assertThat(niveis(37.2, 37.0, 37.5)).containsExactly(4, 4, 5);
    }

    @Test
    void diaAbaixoDoLimiarInterrompeASequencia() {
        assertThat(niveis(35.4, 34.9, 35.2, 35.3)).containsExactly(2, 1, 2, 2);
    }

    @Test
    void diasAnterioresContamParaARegraDeTresDias() {
        assertThat(niveis(List.of(35.5), List.of(35.1, 35.3))).containsExactly(3);
        assertThat(niveis(List.of(35.5), null)).containsExactly(2);
    }

    @Test
    void noMesmoNivelValeARegraDeUmDia() {
        NivelDoDia dia = NivelCalorCalculadora.classificar(LimiaresDeTeste.todos(),
                List.of(36.5, 36.5, 37.1), null).get(2);

        assertThat(dia.nivel()).isEqualTo(4);
        assertThat(dia.regra()).contains("37.0 °C por 1 dia");
        assertThat(dia.nome()).isEqualTo("Alto risco");
        assertThat(dia.dia()).isEqualTo(3);
    }

    @Test
    void ondaDeCalorCompletaSobeEDesce() {
        // aquece, fica 3 dias acima de 37 °C, esfria
        assertThat(niveis(34.0, 35.5, 36.2, 37.3, 37.6, 37.1, 35.8, 33.9))
                .containsExactly(1, 2, 3, 4, 4, 5, 3, 1);
    }
}
