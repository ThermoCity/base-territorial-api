package br.com.thermocity.baseterritorialapi.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

/** Traduz exceções em respostas JSON padronizadas (ApiErro). */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErro> naoEncontrado(RecursoNaoEncontradoException e) {
        log.warn("404: {}", e.getMessage());
        return resposta(404, "Não encontrado", e.getMessage(), null);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ApiErro> requisicaoInvalida(RequisicaoInvalidaException e) {
        log.warn("400: {}", e.getMessage());
        return resposta(400, "Requisição inválida", e.getMessage(), null);
    }

    @ExceptionHandler(ImportacaoInvalidaException.class)
    public ResponseEntity<ApiErro> importacaoInvalida(ImportacaoInvalidaException e) {
        log.warn("422 importação inválida: {} ({} erros, primeiros: {})", e.getMessage(),
                e.getErros() == null ? 0 : e.getErros().size(),
                e.getErros() == null ? List.of() : e.getErros().stream().limit(5).toList());
        return resposta(422, "Importação inválida", e.getMessage(), e.getErros());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErro> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        String mensagem = e.getBindingResult().getFieldErrors().isEmpty()
                ? "Dados inválidos"
                : e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        log.warn("400 validação: {}", detalhes);
        return resposta(400, "Requisição inválida", mensagem, detalhes);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErro> parametros(Exception e) {
        log.warn("400 parâmetros: {}", e.getMessage());
        return resposta(400, "Requisição inválida", "Parâmetros ausentes ou em formato inválido", List.of(e.getMessage()));
    }

    private ResponseEntity<ApiErro> resposta(int status, String erro, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status).body(ApiErro.de(status, erro, mensagem, detalhes));
    }
}
