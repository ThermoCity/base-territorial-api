package br.com.thermocity.baseterritorialapi.buscaativa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Regra v1 da API de cadastro: mesmos pontos e mesmos motivos. */
class RegraPrioridadeTest {

    @Test
    void mesmoResultadoDaApiDeCadastro() {
        // exemplo real de GET /listas/hoje: 70 anos, mobilidade, crônica, setor nota 5, IPVS 5, refúgio a 1,64 km
        RegraPrioridade.Prioridade p = RegraPrioridade.calcular(70, false, true, true, 5, 5, 1640.0);

        assertThat(p.pontos()).isEqualTo(13);
        assertThat(String.join(" · ", p.motivos()))
                .isEqualTo("mobilidade reduzida · condição crônica · 70 anos · setor nota 5 · IPVS 5 · refúgio a 1,6 km");
    }

    @Test
    void outrosExemplosDaListaReal() {
        // mora sozinho, mobilidade, crônica, 69 anos, nota 5, IPVS 4, refúgio a 1,38 km = 13
        assertThat(RegraPrioridade.calcular(69, true, true, true, 4, 5, 1380.0).pontos()).isEqualTo(13);
        // crônica, 92 anos, nota 5, IPVS 5, refúgio a 1,56 km = 11
        assertThat(RegraPrioridade.calcular(92, false, false, true, 5, 5, 1557.0).pontos()).isEqualTo(11);
        // crônica, 83 anos, nota 5, IPVS 4, refúgio a 1,85 km = 10
        assertThat(RegraPrioridade.calcular(83, false, false, true, 4, 5, 1845.0).pontos()).isEqualTo(10);
    }

    @Test
    void maximoEDezesseisPontos() {
        assertThat(RegraPrioridade.calcular(80, true, true, true, 6, 5, null).pontos()).isEqualTo(16);
    }

    @Test
    void semFatoresDeRiscoSoAparecemAIdade() {
        RegraPrioridade.Prioridade p = RegraPrioridade.calcular(64, false, false, false, 2, 1, 300.0);

        assertThat(p.pontos()).isZero();
        assertThat(p.motivos()).containsExactly("64 anos");
    }

    @Test
    void refugioAte800mNaoPontua() {
        assertThat(RegraPrioridade.calcular(75, false, false, false, null, null, 800.0).pontos()).isEqualTo(2);
        assertThat(RegraPrioridade.calcular(75, false, false, false, null, null, 801.0).pontos()).isEqualTo(3);
        assertThat(RegraPrioridade.calcular(75, false, false, false, null, null, null).motivos())
                .contains("sem refúgio considerado");
    }
}
