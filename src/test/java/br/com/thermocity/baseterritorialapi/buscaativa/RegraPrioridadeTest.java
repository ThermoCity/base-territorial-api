package br.com.thermocity.baseterritorialapi.buscaativa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegraPrioridadeTest {

    @Test
    void somaOsPontosEExplicaOsMotivos() {
        RegraPrioridade.Prioridade p = RegraPrioridade.calcular(82, true, true, true, 6, 5, true);

        // 3 (80+) + 2 (sozinho) + 2 (mobilidade) + 1 (crônica) + 2 (nota 5) + 2 (IPVS 6) + 1 (sem refúgio)
        assertThat(p.pontos()).isEqualTo(13);
        assertThat(p.motivos()).containsExactly("80+ anos", "mora sozinho(a)", "mobilidade reduzida",
                "condição crônica", "setor entre os 20% mais quentes", "vulnerabilidade social alta (IPVS 6)",
                "sem refúgio a até 800 m");
    }

    @Test
    void pessoaSemFatoresDeRiscoSoPontuaPelaIdade() {
        RegraPrioridade.Prioridade p = RegraPrioridade.calcular(64, false, false, false, 2, 1, false);

        assertThat(p.pontos()).isEqualTo(1);
        assertThat(p.motivos()).containsExactly("60–69 anos");
    }

    @Test
    void faixasIntermediarias() {
        assertThat(RegraPrioridade.calcular(75, false, false, false, 4, 4, false).pontos()).isEqualTo(2 + 1 + 1);
        assertThat(RegraPrioridade.calcular(75, false, false, false, null, null, false).pontos()).isEqualTo(2);
    }
}
