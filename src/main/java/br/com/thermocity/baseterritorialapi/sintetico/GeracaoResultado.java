package br.com.thermocity.baseterritorialapi.sintetico;

import java.util.Map;

/** Resumo da geração: totais e distribuição, para conferir com o Censo. */
public record GeracaoResultado(
        int quantidade,
        long semente,
        int setoresComPessoas,
        Map<String, Long> porFaixaEtaria,
        Map<String, Long> porSexo,
        long moramSozinhas,
        long mobilidadeReduzida,
        long condicaoCronica,
        String aviso) {
}
