package br.com.thermocity.baseterritorialapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * PBI #33 — Cenário 2: com ddl-auto=validate, um esquema TMC_ divergente impede a subida
 * e a mensagem indica a tabela e a coluna. Usa um H2 próprio, separado dos outros testes.
 */
class EsquemaDivergenteTests {

    private static final String URL = "jdbc:h2:mem:esquema_divergente;MODE=Oracle;DB_CLOSE_DELAY=-1";

    @Test
    void cenario2_esquemaDivergenteImpedeASubida() {
        // 1. Cria o esquema completo e remove uma coluna que a entidade CalorSetor espera.
        try (ConfigurableApplicationContext ctx = subir("create")) {
            ctx.getBean(JdbcTemplate.class).execute("ALTER TABLE TMC_CALOR_SETOR DROP COLUMN NDVI_MEDIO");
        }

        // 2. Sobe de novo só validando: deve falhar apontando tabela e coluna.
        Throwable erro = catchThrowable(() -> subir("validate").close());

        assertThat(erro).isNotNull();
        String pilha = pilha(erro).toLowerCase();
        assertThat(pilha).contains("schema-validation");
        assertThat(pilha).contains("ndvi_medio");
        assertThat(pilha).contains("tmc_calor_setor");
    }

    private static ConfigurableApplicationContext subir(String ddlAuto) {
        return new SpringApplicationBuilder(BaseTerritorialApiApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.datasource.url=" + URL,
                        "spring.jpa.hibernate.ddl-auto=" + ddlAuto,
                        "thermocity.carga-inicial.habilitada=false")
                .run();
    }

    private static String pilha(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
