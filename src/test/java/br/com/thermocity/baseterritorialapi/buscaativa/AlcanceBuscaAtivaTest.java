package br.com.thermocity.baseterritorialapi.buscaativa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Quem entra na lista do dia em cada nível (ação esperada dos limiares A707). */
class AlcanceBuscaAtivaTest {

    @Test
    void nc1ENc2NaoTemBuscaAtiva() {
        assertThat(AlcanceBuscaAtiva.entraNaLista(1, 85, 6)).isFalse();
        assertThat(AlcanceBuscaAtiva.entraNaLista(2, 85, 6)).isFalse();
    }

    @Test
    void nc3So70MaisEmIpvs5e6() {
        assertThat(AlcanceBuscaAtiva.entraNaLista(3, 70, 5)).isTrue();
        assertThat(AlcanceBuscaAtiva.entraNaLista(3, 75, 6)).isTrue();
        assertThat(AlcanceBuscaAtiva.entraNaLista(3, 69, 6)).isFalse();
        assertThat(AlcanceBuscaAtiva.entraNaLista(3, 80, 4)).isFalse();
        assertThat(AlcanceBuscaAtiva.entraNaLista(3, 80, null)).isFalse();
    }

    @Test
    void nc4Todos60MaisEmIpvs4a6() {
        assertThat(AlcanceBuscaAtiva.entraNaLista(4, 60, 4)).isTrue();
        assertThat(AlcanceBuscaAtiva.entraNaLista(4, 60, 3)).isFalse();
        assertThat(AlcanceBuscaAtiva.entraNaLista(4, 90, null)).isFalse();
    }

    @Test
    void nc5TodaACidade() {
        assertThat(AlcanceBuscaAtiva.entraNaLista(5, 60, 1)).isTrue();
        assertThat(AlcanceBuscaAtiva.entraNaLista(5, 60, null)).isTrue();
        assertThat(AlcanceBuscaAtiva.entraNaLista(5, 59, 6)).isFalse();
    }
}
