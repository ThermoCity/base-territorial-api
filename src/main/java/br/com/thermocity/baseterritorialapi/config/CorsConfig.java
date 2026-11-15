package br.com.thermocity.baseterritorialapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera as chamadas do navegador vindas do painel do gêmeo digital (outro domínio).
 * As origens permitidas vêm de CORS_ORIGENS, separadas por vírgula. Ex.: http://localhost:5173,https://painel.exemplo.com
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origens;

    public CorsConfig(@Value("${thermocity.cors.origens}") String[] origens) {
        // tira espaços em volta de cada origem ("a, b" vira "a" e "b"); com espaço a origem não bate e o navegador bloqueia
        this.origens = java.util.Arrays.stream(origens).map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origens)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
