package br.com.thermocity.baseterritorialapi.common;

import java.util.List;

/** Vira HTTP 422: o arquivo foi lido, mas tem registros inválidos. Nada é gravado. */
public class ImportacaoInvalidaException extends RuntimeException {

    private final List<String> erros;

    public ImportacaoInvalidaException(String mensagem, List<String> erros) {
        super(mensagem);
        this.erros = List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}
