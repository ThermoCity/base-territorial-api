package br.com.thermocity.baseterritorialapi.simulacao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import br.com.thermocity.baseterritorialapi.sintetico.PessoaSinteticaRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PBI: Criar e consultar cenários de simulação. */
@SpringBootTest
class SimulacoesIntegrationTests {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private LimiarNivelCalorRepository limiares;

    @Autowired
    private ResumoDiarioClimaRepository resumos;

    @Autowired
    private SimulacaoRepository simulacoes;

    @Autowired
    private SimulacaoDiaRepository dias;

    @Autowired
    private SimulacaoResultadoRepository resultados;

    @Autowired
    private PessoaSinteticaRepository pessoas;

    @Autowired
    private SetorRepository setores;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        resultados.deleteAllInBatch();
        dias.deleteAllInBatch();
        simulacoes.deleteAllInBatch();
        // sem pessoas: aqui só interessa o cenário e o nível (o motor tem testes próprios)
        pessoas.deleteAllInBatch();
        limiares.deleteAll();
        limiares.saveAll(LimiaresDeTeste.todos());
        resumos.deleteAll();
        // máximo histórico da série: 40,9 °C
        resumos.save(new ResumoDiarioClima(new ResumoDiarioClimaId(LocalDate.of(2024, 3, 18), "INMET", "OBSERVACAO"),
                new BigDecimal("40.90"), 'S'));
    }

    private ResultActions criar(String json) throws Exception {
        return mvc.perform(post("/simulacoes")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cenarioAceitoDevolveIdEOResultadoFicaDisponivel() throws Exception {
        String resposta = criar("""
                { "nome": "Setembro de 2023 + 3 °C", "modeloId": "A707-2023-09-22+3",
                  "icMaxPorDia": [35.4, 35.8, 35.2], "agentesPorUbs": 2, "dataInicio": "2026-12-01" }
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/simulacoes/\\d+$")))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.qtdDias").value(3))
                .andExpect(jsonPath("$.nivelMaximo").value(3))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");

        mvc.perform(get("/simulacoes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Setembro de 2023 + 3 °C"))
                .andExpect(jsonPath("$.cenario.modeloId").value("A707-2023-09-22+3"))
                .andExpect(jsonPath("$.cenario.agentesPorUbs").value(2))
                // padrões do cenário
                .andExpect(jsonPath("$.cenario.refugios").value("TODOS"))
                .andExpect(jsonPath("$.cenario.contatosPorAgenteDia").value(15))
                .andExpect(jsonPath("$.cenario.fatorK").value(0.25))
                .andExpect(jsonPath("$.versaoLimiar").value(LimiaresDeTeste.VERSAO))
                .andExpect(jsonPath("$.dias", hasSize(3)))
                .andExpect(jsonPath("$.dias[0].nivel").value(2))
                .andExpect(jsonPath("$.dias[1].nivel").value(2))
                // persistência: 3 dias ≥ 35,0 °C sobem o dia 3 para NC3, igual ao modo real
                .andExpect(jsonPath("$.dias[2].nivel").value(3))
                .andExpect(jsonPath("$.dias[2].regra").value(containsString("3 dia")))
                .andExpect(jsonPath("$.dias[2].data").value("2026-12-03"))
                .andExpect(jsonPath("$.dias[2].icMaxC").value(35.2))
                // sem pessoas cadastradas, a lista fica vazia; há uma linha por setor em cada dia
                .andExpect(jsonPath("$.dias[2].pessoasNaLista").value(0))
                .andExpect(jsonPath("$.setores", hasSize((int) setores.count() * 3)));
    }

    @Test
    void diaAcimaDoMaximoHistoricoFicaMarcadoComoExtrapolacao() throws Exception {
        String resposta = criar("{ \"icMaxPorDia\": [41.5, 40.9], \"agentesPorUbs\": 1 }")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diasExtrapolados").value(1))
                .andExpect(jsonPath("$.nome").value("Simulação de 2 dias"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");

        mvc.perform(get("/simulacoes/" + id))
                .andExpect(jsonPath("$.maxHistoricoIcC").value(40.9))
                .andExpect(jsonPath("$.dias[0].extrapolacao").value(true))
                .andExpect(jsonPath("$.dias[1].extrapolacao").value(false));
    }

    @Test
    void cenarioInvalidoDevolve400IndicandoOCampo() throws Exception {
        criar("{ \"icMaxPorDia\": [35.0, null], \"agentesPorUbs\": 2 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem(containsString("icMaxPorDia[1]"))));
        criar("{ \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 0 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem(containsString("agentesPorUbs"))));
        criar("{ \"icMaxPorDia\": [35.0] }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem(containsString("agentesPorUbs"))));
        criar("{ \"icMaxPorDia\": [35,35,35,35,35,35,35,35,35,35,35,35,35,35,35,35], \"agentesPorUbs\": 2 }")
                .andExpect(status().isBadRequest());
        criar("{ \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 2, \"fatorK\": 0.6 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem(containsString("fatorK"))));
        criar("{ \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 2, \"refugios\": \"ALGUNS\" }")
                .andExpect(status().isBadRequest());
        criar("{ \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 2, \"distrito\": \"Atlântida\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("distrito")));
        assertThat(simulacoes.count()).isZero();
    }

    @Test
    void recorteDeDistritoExistenteEAceito() throws Exception {
        criar("{ \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 2, \"distrito\": \"presidente prudente\", \"refugios\": \"NENHUM\" }")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.distrito").value("presidente prudente"))
                .andExpect(jsonPath("$.refugios").value("NENHUM"));
    }

    @Test
    void simulacaoInexistenteDevolve404() throws Exception {
        mvc.perform(get("/simulacoes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(containsString("999999")));
    }

    @Test
    void listaPaginadaDaMaisRecenteParaAMaisAntiga() throws Exception {
        criar("{ \"nome\": \"Primeira\", \"icMaxPorDia\": [35.0], \"agentesPorUbs\": 2 }").andExpect(status().isCreated());
        criar("{ \"nome\": \"Segunda\", \"icMaxPorDia\": [37.0], \"agentesPorUbs\": 2 }").andExpect(status().isCreated());

        mvc.perform(get("/simulacoes").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Segunda"))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void rodarUmaSimulacaoNaoAlteraOsDadosReais() throws Exception {
        long limiaresAntes = limiares.count();
        long resumosAntes = resumos.count();

        criar("{ \"icMaxPorDia\": [38.0, 39.0, 41.0], \"agentesPorUbs\": 3 }").andExpect(status().isCreated());

        assertThat(limiares.count()).isEqualTo(limiaresAntes);
        assertThat(resumos.count()).isEqualTo(resumosAntes);
    }
}
