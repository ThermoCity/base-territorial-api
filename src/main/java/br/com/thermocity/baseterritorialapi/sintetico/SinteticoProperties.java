package br.com.thermocity.baseterritorialapi.sintetico;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Taxas usadas para sortear os atributos das pessoas fictícias.
 * ATENÇÃO: valores PROVISÓRIOS, definidos em application.properties. Não são estatísticas oficiais.
 */
@ConfigurationProperties("thermocity.sintetico")
public record SinteticoProperties(
        long sementePadrao,
        double probabilidadeFeminino,
        double taxaMoraSozinho,
        double taxaMobilidadeReduzida,
        double taxaCondicaoCronica) {
}
