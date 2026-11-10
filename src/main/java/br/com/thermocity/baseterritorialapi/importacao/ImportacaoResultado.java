package br.com.thermocity.baseterritorialapi.importacao;

import java.util.Map;

/** Resumo de uma importação bem-sucedida. */
public record ImportacaoResultado(String tipo, int registros, Map<String, Long> resumo) {
}
