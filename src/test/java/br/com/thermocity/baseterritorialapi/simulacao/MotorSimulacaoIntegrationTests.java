package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.sintetico.PessoaSintetica;
import br.com.thermocity.baseterritorialapi.sintetico.PessoaSinteticaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PBI 107: simulação dia a dia sobre o cadastro sintético (300 pessoas 60+). */
@SpringBootTest
class MotorSimulacaoIntegrationTests {

    private static final int PESSOAS = 300;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private LimiarNivelCalorRepository limiares;

    @Autowired
    private PessoaSinteticaRepository pessoas;

    private MockMvc mvc;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        limiares.deleteAll();
        limiares.saveAll(LimiaresDeTeste.todos());
        mvc.perform(post("/admin/sinteticos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": " + PESSOAS + ", \"semente\": 42}"))
                .andExpect(status().isCreated());
    }

    /** Cria a simulação e devolve o resultado completo (GET /simulacoes/{id}). */
    private String simular(String json) throws Exception {
        String criada = mvc.perform(post("/simulacoes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(criada, "$.id");
        return mvc.perform(get("/simulacoes/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void emergenciaPoeTodaACidadeNaListaEAplicaACapacidadeDasUbs() throws Exception {
        // NC5 nos 2 dias; 1 agente × 1 contato = 1 contato por UBS por dia
        String r = simular("{ \"icMaxPorDia\": [38.6, 38.6], \"agentesPorUbs\": 1, \"contatosPorAgenteDia\": 1 }");

        int lista1 = JsonPath.read(r, "$.dias[0].pessoasNaLista");
        int contatadas1 = JsonPath.read(r, "$.dias[0].contatadas");
        int pendentes1 = JsonPath.read(r, "$.dias[0].pendentes");
        assertThat(lista1).isEqualTo(PESSOAS);
        assertThat(contatadas1).isPositive().isLessThanOrEqualTo(37); // no máximo 1 por UBS (36 UBS + sem UBS)
        assertThat(contatadas1 + pendentes1).isEqualTo(PESSOAS);
        // ninguém aparece duas vezes na fila do dia 2
        assertThat((int) JsonPath.read(r, "$.dias[1].pessoasNaLista")).isEqualTo(PESSOAS);

        // a soma dos setores bate com o total do dia
        List<Integer> porSetor = JsonPath.read(r, "$.setores[?(@.dia == 1)].pessoasNaLista");
        assertThat(porSetor.stream().mapToInt(Integer::intValue).sum()).isEqualTo(PESSOAS);
    }

    @Test
    void capacidadeSuficienteContataTodoMundo() throws Exception {
        String r = simular("{ \"icMaxPorDia\": [38.6], \"agentesPorUbs\": 50, \"contatosPorAgenteDia\": 100 }");

        assertThat((int) JsonPath.read(r, "$.dias[0].contatadas")).isEqualTo(PESSOAS);
        assertThat((int) JsonPath.read(r, "$.dias[0].pendentes")).isZero();
    }

    @Test
    void semRefugiosTodosOsIdososDaListaFicamSemRefugioProximo() throws Exception {
        String r = simular("{ \"icMaxPorDia\": [38.6, 37.2], \"agentesPorUbs\": 2, \"refugios\": \"NENHUM\" }");

        for (int dia = 0; dia < 2; dia++) {
            int lista = JsonPath.read(r, "$.dias[" + dia + "].pessoasNaLista");
            assertThat(lista).isPositive();
            assertThat((int) JsonPath.read(r, "$.dias[" + dia + "].semRefugio800m")).isEqualTo(lista);
        }
    }

    @Test
    void comRefugiosNemTodosFicamSemRefugio() throws Exception {
        String r = simular("{ \"icMaxPorDia\": [38.6], \"agentesPorUbs\": 2, \"refugios\": \"TODOS\" }");

        assertThat((int) JsonPath.read(r, "$.dias[0].semRefugio800m")).isLessThan(PESSOAS);
    }

    @Test
    void diasSemBuscaAtivaTemListaVazia() throws Exception {
        String r = simular("{ \"icMaxPorDia\": [30.0, 35.0], \"agentesPorUbs\": 2 }");

        assertThat((int) JsonPath.read(r, "$.dias[0].pessoasNaLista")).isZero();
        assertThat((int) JsonPath.read(r, "$.dias[1].pessoasNaLista")).isZero();
    }

    @Test
    void rodarASimulacaoNaoAlteraOCadastroReal() throws Exception {
        List<String> antes = pessoas.findAll().stream().map(MotorSimulacaoIntegrationTests::resumo).sorted().toList();
        long limiaresAntes = limiares.count();

        simular("{ \"icMaxPorDia\": [38.6, 38.6, 38.6], \"agentesPorUbs\": 1, \"refugios\": \"NENHUM\" }");

        assertThat(pessoas.findAll().stream().map(MotorSimulacaoIntegrationTests::resumo).sorted().toList())
                .isEqualTo(antes);
        assertThat(limiares.count()).isEqualTo(limiaresAntes);
    }

    private static String resumo(PessoaSintetica p) {
        return p.getId() + "|" + p.getIdade() + "|" + p.getSetorCodigo() + "|" + p.getUbsReferenciaId() + "|"
                + p.isMoraSozinho() + "|" + p.isMobilidadeReduzida() + "|" + p.isCondicaoCronica();
    }

    @Test
    void mesmoCenarioMesmoResultado() throws Exception {
        String cenario = "{ \"icMaxPorDia\": [37.5, 38.6], \"agentesPorUbs\": 1, \"contatosPorAgenteDia\": 3 }";
        String a = simular(cenario);
        String b = simular(cenario);

        assertThat((Object) JsonPath.read(a, "$.dias")).isEqualTo(JsonPath.read(b, "$.dias"));
        assertThat((Object) JsonPath.read(a, "$.setores")).isEqualTo(JsonPath.read(b, "$.setores"));
    }
}
