package br.com.thermocity.baseterritorialapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes ponta a ponta da feature "Base Territorial de Presidente Prudente".
 * Banco H2 em memória, carregado pelos CSVs de src/main/resources/dados (420 setores, IPVS, 191 serviços).
 * Os nomes seguem os cenários dos PBIs do Azure Boards.
 */
@SpringBootTest
class BaseTerritorialIntegrationTests {

    private static final String SETOR_CENTRO = "354140605000002";
    private static final String SETOR_SEM_IPVS = "354140605000019";
    private static final String SETOR_MAIS_QUENTE = "354140605000114";
    private static final String SETOR_PARA_SEM_DADO = "354140605000003";
    private static final String CABECALHO_CALOR = "codigo_setor;lst_verao_c;ndvi_medio;pct_construcoes;pct_vegetacao_densa;"
            + "ta_estimada_c;classe_calor;pixels_validos;cena_landsat;data_cena\n";
    private static final String CENA = "LC08_L2SP_222075_20240108_20240122_02_T1";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // ---------------------------------------------------------------- PBI #3: setores + Censo

    @Test
    void consultaSetorComPopulacaoDoCenso() throws Exception {
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(SETOR_CENTRO))
                .andExpect(jsonPath("$.populacao.total").value(419))
                .andExpect(jsonPath("$.populacao.de60a69").value(44))
                .andExpect(jsonPath("$.populacao.de70mais").value(103))
                .andExpect(jsonPath("$.populacao.de60mais").value(147));
    }

    @Test
    void setorInexistenteRetorna404() throws Exception {
        mvc.perform(get("/setores/999999999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Setor não encontrado"));
    }

    @Test
    void importacaoDeSetoresComErroNaoGravaNada() throws Exception {
        String csv = "codigo;pop_total;pop_60_69;pop_70mais;dado_censo;geometria_wkt\n"
                + "123;10;1;1;ok;POLYGON((0 0,1 0,1 1,0 0))\n"
                + "354140605000002;10;1;1;ok;POLYGON((0 0,1 0,1 1,0 0))\n";
        mvc.perform(multipart("/admin/importacoes/setores").file(arquivo(csv)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes", hasSize(1)));

        mvc.perform(get("/setores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(420)));
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(jsonPath("$.populacao.total").value(419));
    }

    @Test
    void importacaoSemArquivoRetorna400() throws Exception {
        mvc.perform(multipart("/admin/importacoes/setores").file(arquivo("")))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- PBI #4: IPVS

    @Test
    void setorTemGrupoIpvs() throws Exception {
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(jsonPath("$.ipvs.grupo").value(2))
                .andExpect(jsonPath("$.ipvs.status").value("OK"));
    }

    @Test
    void setorNaoClassificadoFicaSemDado() throws Exception {
        mvc.perform(get("/setores/" + SETOR_SEM_IPVS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipvs.grupo").value(nullValue()))
                .andExpect(jsonPath("$.ipvs.status").value("SEM_DADO"));
    }

    @Test
    void grupoIpvsForaDaFaixaRetorna422() throws Exception {
        String csv = "codigo_setor;grupo;nome;edicao\n" + SETOR_CENTRO + ";7;Inventado;2022\n";
        mvc.perform(multipart("/admin/importacoes/ipvs").file(arquivo(csv)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(jsonPath("$.ipvs.grupo").value(2));
    }

    // ---------------------------------------------------------------- PBI #5: UBS e refúgios

    @Test
    void servicosDoSetor() throws Exception {
        mvc.perform(get("/setores/" + SETOR_CENTRO + "/servicos").param("raioMetros", "1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ubsMaisProxima.tipo").value("UBS"))
                .andExpect(jsonPath("$.ubsMaisProxima.distanciaMetros").isNumber())
                .andExpect(jsonPath("$.raioMetros").value(1500));
    }

    @Test
    void raioInvalidoRetorna400() throws Exception {
        mvc.perform(get("/setores/" + SETOR_CENTRO + "/servicos").param("raioMetros", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refugioMaisProximoDeUmPonto() throws Exception {
        mvc.perform(get("/refugios/proximo").param("lat", "-22.125").param("lon", "-51.389"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("REFUGIO"))
                .andExpect(jsonPath("$.revisao").value("manter"))
                .andExpect(jsonPath("$.distanciaMetros").isNumber());
    }

    @Test
    void coordenadaForaDoMunicipioRetorna400() throws Exception {
        // Praça da Sé, São Paulo
        mvc.perform(get("/refugios/proximo").param("lat", "-23.5503").param("lon", "-46.6339"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Coordenada fora da área atendida"));
    }

    // ---------------------------------------------------------------- PBI #6: cadastro sintético

    @Test
    void geraCadastroSinteticoEConsultaPessoa() throws Exception {
        mvc.perform(post("/admin/sinteticos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 500, \"semente\": 7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidade").value(500))
                .andExpect(jsonPath("$.semente").value(7))
                .andExpect(jsonPath("$.setoresComPessoas").value(greaterThan(100)))
                .andExpect(jsonPath("$.moramSozinhas").value(lessThanOrEqualTo(500)));

        mvc.perform(get("/pessoas").param("setor", SETOR_CENTRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sintetico").value(true))
                .andExpect(jsonPath("$[0].setorCodigo").value(SETOR_CENTRO))
                .andExpect(jsonPath("$[0].ubsReferenciaNome").isString());
    }

    @Test
    void quantidadeZeroRetorna400() throws Exception {
        mvc.perform(post("/admin/sinteticos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Quantidade deve ser maior que zero"));
    }

    // ---------------------------------------------------------------- PBI #11: calor por setor

    @Test
    void setorTemBlocoCalor() throws Exception {
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calor.classe").value(3))
                .andExpect(jsonPath("$.calor.temperaturaSuperficieC").value(45.78))
                .andExpect(jsonPath("$.calor.cenaLandsat").value(CENA))
                .andExpect(jsonPath("$.calor.dataCena").value("2024-01-08"))
                .andExpect(jsonPath("$.calor.status").value("OK"));
        mvc.perform(get("/setores/" + SETOR_MAIS_QUENTE))
                .andExpect(jsonPath("$.calor.classe").value(5))
                .andExpect(jsonPath("$.calor.temperaturaSuperficieC").value(49.01));
    }

    @Test
    void classeDeCalorForaDaFaixaRetorna422() throws Exception {
        String csv = CABECALHO_CALOR + SETOR_CENTRO + ";45.78;0.172;83.7;0.1;29.97;6;114;" + CENA + ";2024-01-08\n";
        mvc.perform(multipart("/admin/importacoes/calor").file(arquivo(csv)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes", hasSize(1)));
        mvc.perform(get("/setores/" + SETOR_CENTRO))
                .andExpect(jsonPath("$.calor.classe").value(3));
    }

    @Test
    void calorDeSetorInexistenteRetorna422() throws Exception {
        String csv = CABECALHO_CALOR + "999999999999999;45.0;0.2;50;1;25;3;10;" + CENA + ";2024-01-08\n";
        mvc.perform(multipart("/admin/importacoes/calor").file(arquivo(csv)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void setorSemPixelValidoFicaSemDado() throws Exception {
        String semDado = CABECALHO_CALOR + SETOR_PARA_SEM_DADO + ";;;;;;;0;" + CENA + ";2024-01-08\n";
        mvc.perform(multipart("/admin/importacoes/calor").file(arquivo(semDado)))
                .andExpect(status().isCreated());
        mvc.perform(get("/setores/" + SETOR_PARA_SEM_DADO))
                .andExpect(jsonPath("$.calor.status").value("SEM_DADO"))
                .andExpect(jsonPath("$.calor.classe").value(nullValue()));

        // devolve o valor real para não afetar os outros testes
        String original = CABECALHO_CALOR + SETOR_PARA_SEM_DADO + ";45.79;0.192;83.0;0.1;29.50;3;156;" + CENA + ";2024-01-08\n";
        mvc.perform(multipart("/admin/importacoes/calor").file(arquivo(original)))
                .andExpect(status().isCreated());
    }

    @Test
    void geojsonTrazTodosOsSetoresComClasseDeCalor() throws Exception {
        mvc.perform(get("/setores/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features", hasSize(420)))
                .andExpect(jsonPath("$.features[0].geometry.type").isString())
                .andExpect(jsonPath("$.features[?(@.id == '" + SETOR_MAIS_QUENTE + "')].properties.classeCalor").value(5));
    }

    @Test
    void geojsonTrazOsCamposUsadosPeloPainel() throws Exception {
        mvc.perform(get("/setores/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[0].properties.situacao").exists())
                .andExpect(jsonPath("$.features[0].properties.distrito").exists())
                .andExpect(jsonPath("$.features[0].properties.pop70mais").exists());
    }

    @Test
    void servicosTrazUbsERefugiosSugeriveisParaOMapa() throws Exception {
        mvc.perform(get("/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.tipo == 'UBS')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.tipo == 'REFUGIO' && @.revisao == 'remover')]").isEmpty())
                .andExpect(jsonPath("$[?(@.tipo == 'REFUGIO' && @.revisao == 'revisar')]").isEmpty());
    }

    @Test
    void corsLiberaOPainelLocal() throws Exception {
        mvc.perform(options("/servicos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private static MockMultipartFile arquivo(String conteudo) {
        return new MockMultipartFile("arquivo", "dados.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
