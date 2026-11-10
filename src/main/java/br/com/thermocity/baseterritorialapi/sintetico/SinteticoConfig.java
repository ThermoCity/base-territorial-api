package br.com.thermocity.baseterritorialapi.sintetico;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SinteticoProperties.class)
public class SinteticoConfig {
}
