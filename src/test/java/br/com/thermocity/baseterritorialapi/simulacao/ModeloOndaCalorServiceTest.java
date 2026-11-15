package br.com.thermocity.baseterritorialapi.simulacao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Episódios NC4/NC5 da série histórica e variações com acréscimo no IC. */
class ModeloOndaCalorServiceTest {

    /** Série com dois trechos separados por um buraco de datas. */
    static List<DiaClima> serie() {
        List<DiaClima> s = new ArrayList<>();
        // trecho antigo: um único dia em NC5 (40,9 °C, o máximo histórico)
        adicionar(s, LocalDate.of(2020, 1, 8), 33.0, 34.0, 40.9, 33.0);
        // trecho novo: onda de 4 dias (03 a 06/03/2023) com entrada e saída
        adicionar(s, LocalDate.of(2023, 3, 1), 34.0, 35.0, 37.5, 38.0, 38.6, 36.0, 33.0, 32.0);
        return s;
    }

    static void adicionar(List<DiaClima> s, LocalDate inicio, double... ic) {
        for (int i = 0; i < ic.length; i++) s.add(new DiaClima(inicio.plusDays(i), ic[i]));
    }

    private static ModelosOndaCalorResponse montar(List<DiaClima> serie, List<Double> acrescimos, int duracaoMinima) {
        return ModeloOndaCalorService.montar(LimiaresDeTeste.todos(), serie, acrescimos, duracaoMinima, "teste");
    }

    @Test
    void episodioRealTrazDatasEIcDeCadaDia() {
        ModelosOndaCalorResponse r = montar(serie(), List.of(1.0, 3.0), 3);

        assertThat(r.maxHistoricoC()).isEqualTo(40.9);
        assertThat(r.versaoLimiar()).isEqualTo(LimiaresDeTeste.VERSAO);
        assertThat(r.modelos()).extracting(ModelosOndaCalorResponse.Modelo::id)
                .containsExactly("A707-2023-03-03", "A707-2023-03-03+1", "A707-2023-03-03+3");

        ModelosOndaCalorResponse.Modelo real = r.modelos().get(0);
        assertThat(real.tipo()).isEqualTo("REAL");
        assertThat(real.nome()).isEqualTo("Onda de calor de 03/03 a 06/03/2023");
        assertThat(real.inicioEpisodio()).isEqualTo(LocalDate.of(2023, 3, 3));
        assertThat(real.fimEpisodio()).isEqualTo(LocalDate.of(2023, 3, 6));
        assertThat(real.diasEpisodio()).isEqualTo(4);
        assertThat(real.nivelMaximo()).isEqualTo(5);
        assertThat(real.icMaxC()).isEqualTo(38.6);
        assertThat(real.diasExtrapolados()).isZero();
        // 2 dias de entrada + 4 do episódio + 2 de saída
        assertThat(real.dias()).extracting(ModelosOndaCalorResponse.Dia::icMaxC)
                .containsExactly(34.0, 35.0, 37.5, 38.0, 38.6, 36.0, 33.0, 32.0);
        assertThat(real.dias()).extracting(ModelosOndaCalorResponse.Dia::nivel)
                .containsExactly(1, 2, 4, 4, 5, 4, 1, 1);
        assertThat(real.dias()).extracting(ModelosOndaCalorResponse.Dia::parteDoEpisodio)
                .containsExactly(false, false, true, true, true, true, false, false);
        assertThat(real.dias().get(0).data()).isEqualTo(LocalDate.of(2023, 3, 1));
    }

    @Test
    void variacaoAcimaDoHistoricoMarcaExtrapolacao() {
        ModelosOndaCalorResponse.Modelo mais3 = montar(serie(), List.of(1.0, 3.0), 3).modelos().get(2);

        assertThat(mais3.tipo()).isEqualTo("VARIACAO");
        assertThat(mais3.acrescimoC()).isEqualTo(3.0);
        assertThat(mais3.nome()).isEqualTo("Onda de calor de 03/03 a 06/03/2023 + 3 °C");
        assertThat(mais3.dias()).extracting(ModelosOndaCalorResponse.Dia::icMaxC)
                .containsExactly(37.0, 38.0, 40.5, 41.0, 41.6, 39.0, 36.0, 35.0);
        // nível recalculado com o calor maior (a persistência sobe a saída da onda)
        assertThat(mais3.dias()).extracting(ModelosOndaCalorResponse.Dia::nivel)
                .containsExactly(4, 4, 5, 5, 5, 5, 4, 3);
        // só passa de 40,9 °C nos dias 4 e 5
        assertThat(mais3.dias()).extracting(ModelosOndaCalorResponse.Dia::extrapolacao)
                .containsExactly(false, false, false, true, true, false, false, false);
        assertThat(mais3.diasExtrapolados()).isEqualTo(2);
    }

    @Test
    void duracaoMinimaFiltraEpisodiosCurtosEOrdenaDoMaisRecente() {
        ModelosOndaCalorResponse r = montar(serie(), List.of(), 1);

        assertThat(r.modelos()).extracting(ModelosOndaCalorResponse.Modelo::id)
                .containsExactly("A707-2023-03-03", "A707-2020-01-10");
        ModelosOndaCalorResponse.Modelo antigo = r.modelos().get(1);
        assertThat(antigo.nome()).isEqualTo("Onda de calor de 10/01/2020");
        // o buraco de datas limita a janela ao próprio trecho
        assertThat(antigo.dias()).hasSize(4);
    }

    @Test
    void buracoNaSerieInterrompeOEpisodio() {
        List<DiaClima> s = new ArrayList<>();
        adicionar(s, LocalDate.of(2024, 3, 1), 37.5, 37.5);
        adicionar(s, LocalDate.of(2024, 3, 4), 37.5, 37.5); // falta o dia 03/03

        assertThat(montar(s, List.of(), 3).modelos()).isEmpty();
        assertThat(montar(s, List.of(), 2).modelos()).hasSize(2);
    }

    @Test
    void episodioLongoLimitaOModeloA15Dias() {
        List<DiaClima> s = new ArrayList<>();
        double[] ic = new double[24];
        java.util.Arrays.fill(ic, 37.5);
        ic[0] = ic[1] = ic[22] = ic[23] = 33.0;
        adicionar(s, LocalDate.of(2023, 11, 1), ic);

        ModelosOndaCalorResponse.Modelo m = montar(s, List.of(), 3).modelos().get(0);
        assertThat(m.diasEpisodio()).isEqualTo(20);
        assertThat(m.dias()).hasSize(15);
        assertThat(m.dias().get(0).data()).isEqualTo(LocalDate.of(2023, 11, 1));
    }

    @Test
    void serieVaziaNaoTemModelos() {
        ModelosOndaCalorResponse r = montar(List.of(), List.of(3.0), 3);
        assertThat(r.modelos()).isEmpty();
        assertThat(r.maxHistoricoC()).isNull();
    }
}
