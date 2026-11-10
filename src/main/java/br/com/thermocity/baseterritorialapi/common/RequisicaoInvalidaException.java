package br.com.thermocity.baseterritorialapi.common;

/** Vira HTTP 400. */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
