package br.com.thermocity.baseterritorialapi.servico;

import java.util.List;

/**
 * Serviços que atendem um setor. As distâncias partem do ponto de referência do setor
 * e são em linha reta (não pelo caminho das ruas).
 */
public record ServicosDoSetorResponse(
        String codigoSetor,
        ServicoResponse ubsMaisProxima,
        ServicoResponse refugioMaisProximo,
        Integer raioMetros,
        List<ServicoResponse> servicosNoRaio) {
}
