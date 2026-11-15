package br.com.thermocity.baseterritorialapi.simulacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PBI: Calcular o nível de calor na simulação a partir dos limiares do banco. */
@SpringBootTest
class SimulacaoNiveisIntegrationTests {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private LimiarNivelCalorRepository limiares;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        limiares.deleteAll();
        limiares.saveAll(LimiaresDeTeste.todos());
    }

    private org.springframework.test.web.servlet.ResultActions simular(String json) throws Exception {
        return mvc.perform(post("/simulacoes/niveis").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void calculaONivelDeCadaDiaPelosLimiaresDoBanco() throws Exception {
        simular("""
                { "icMaxPorDia": [34.6, 37.2, 35.4, 35.8, 35.2], "dataInicio": "2026-12-01" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoLimiar").value(LimiaresDeTeste.VERSAO))
                .andExpect(jsonPath("$.dias", hasSize(5)))
                .andExpect(jsonPath("$.dias[0].nivel").value(1))
                .andExpect(jsonPath("$.dias[0].nome").value("Normal"))
                .andExpect(jsonPath("$.dias[0].data").value("2026-12-01"))
                .andExpect(jsonPath("$.dias[1].nivel").value(4))
                .andExpect(jsonPath("$.dias[1].icMaxC").value(37.2))
                .andExpect(jsonPath("$.dias[4].nivel").value(3))
                .andExpect(jsonPath("$.dias[4].dia").value(5))
                .andExpect(jsonPath("$.dias[4].data").value("2026-12-05"))
                .andExpect(jsonPath("$.dias[4].regra").value(org.hamcrest.Matchers.containsString("3 dia")));
    }

    @Test
    void semDataInicioOsDiasVemSemData() throws Exception {
        simular("{ \"icMaxPorDia\": [35.0] }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].nivel").value(2))
                .andExpect(jsonPath("$.dias[0].data").value(nullValue()));
    }

    @Test
    void diasAnterioresAntecipamARegraDePersistencia() throws Exception {
        simular("{ \"icMaxPorDia\": [37.1], \"icMaxDiasAnteriores\": [37.4, 37.0] }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias", hasSize(1)))
                .andExpect(jsonPath("$.dias[0].nivel").value(5));
    }

    @Test
    void cenarioSemDiasDevolve400() throws Exception {
        simular("{ \"icMaxPorDia\": [] }").andExpect(status().isBadRequest());
    }

    @Test
    void diaSemIcOuForaDaFaixaDevolve400() throws Exception {
        simular("{ \"icMaxPorDia\": [35.0, null] }").andExpect(status().isBadRequest());
        simular("{ \"icMaxPorDia\": [80.0] }").andExpect(status().isBadRequest());
    }

    @Test
    void semLimiaresAtivosDevolve400() throws Exception {
        limiares.deleteAll();

        simular("{ \"icMaxPorDia\": [36.0] }").andExpect(status().isBadRequest());
    }
}
