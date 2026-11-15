package br.com.thermocity.baseterritorialapi.simulacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PBI: Disponibilizar modelos prontos de onda de calor. */
@SpringBootTest
class ModelosOndaCalorIntegrationTests {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private LimiarNivelCalorRepository limiares;

    @Autowired
    private ResumoDiarioClimaRepository resumos;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        limiares.deleteAll();
        limiares.saveAll(LimiaresDeTeste.todos());
        resumos.deleteAll();

        List<ResumoDiarioClima> linhas = new ArrayList<>();
        for (DiaClima d : ModeloOndaCalorServiceTest.serie()) linhas.add(resumo(d.data(), "INMET", "OBSERVACAO", d.icMaxC(), 'S'));
        // não entram: dia inválido e outra fonte (senão o máximo histórico mudaria)
        linhas.add(resumo(LocalDate.of(2023, 3, 20), "INMET", "OBSERVACAO", 45.0, 'N'));
        linhas.add(resumo(LocalDate.of(2023, 3, 3), "OPENWEATHER", "PREVISAO", 50.0, 'S'));
        resumos.saveAll(linhas);
    }

    private static ResumoDiarioClima resumo(LocalDate data, String fonte, String tipo, double ic, char valido) {
        return new ResumoDiarioClima(new ResumoDiarioClimaId(data, fonte, tipo), BigDecimal.valueOf(ic), valido);
    }

    @Test
    void listaOsEpisodiosReaisComDatasEIcDeCadaDia() throws Exception {
        mvc.perform(get("/simulacoes/modelos"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(jsonPath("$.versaoLimiar").value(LimiaresDeTeste.VERSAO))
                .andExpect(jsonPath("$.maxHistoricoC").value(40.9))
                .andExpect(jsonPath("$.modelos", hasSize(3)))
                .andExpect(jsonPath("$.modelos[0].id").value("A707-2023-03-03"))
                .andExpect(jsonPath("$.modelos[0].tipo").value("REAL"))
                .andExpect(jsonPath("$.modelos[0].inicioEpisodio").value("2023-03-03"))
                .andExpect(jsonPath("$.modelos[0].fimEpisodio").value("2023-03-06"))
                .andExpect(jsonPath("$.modelos[0].dias", hasSize(8)))
                .andExpect(jsonPath("$.modelos[0].dias[4].data").value("2023-03-05"))
                .andExpect(jsonPath("$.modelos[0].dias[4].icMaxC").value(38.6))
                .andExpect(jsonPath("$.modelos[0].dias[4].nivel").value(5));
    }

    @Test
    void variacaoMais3MarcaOsDiasAcimaDoHistoricoComoExtrapolacao() throws Exception {
        mvc.perform(get("/simulacoes/modelos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelos[2].id").value("A707-2023-03-03+3"))
                .andExpect(jsonPath("$.modelos[2].nome").value("Onda de calor de 03/03 a 06/03/2023 + 3 °C"))
                .andExpect(jsonPath("$.modelos[2].diasExtrapolados").value(2))
                .andExpect(jsonPath("$.modelos[2].dias[3].icMaxC").value(41.0))
                .andExpect(jsonPath("$.modelos[2].dias[3].extrapolacao").value(true))
                .andExpect(jsonPath("$.modelos[2].dias[2].extrapolacao").value(false));
    }

    @Test
    void acrescimosEDuracaoPodemSerEscolhidos() throws Exception {
        mvc.perform(get("/simulacoes/modelos").param("acrescimos", "2").param("duracaoMinima", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelos", hasSize(4)))
                .andExpect(jsonPath("$.modelos[1].id").value("A707-2023-03-03+2"))
                .andExpect(jsonPath("$.modelos[2].id").value("A707-2020-01-10"));
    }

    @Test
    void parametrosForaDaFaixaDevolvem400() throws Exception {
        mvc.perform(get("/simulacoes/modelos").param("acrescimos", "9")).andExpect(status().isBadRequest());
        mvc.perform(get("/simulacoes/modelos").param("acrescimos", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/simulacoes/modelos").param("duracaoMinima", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/simulacoes/modelos").param("acrescimos", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void semSerieHistoricaAListaVemVazia() throws Exception {
        resumos.deleteAll();
        mvc.perform(get("/simulacoes/modelos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelos", hasSize(0)));
    }
}
