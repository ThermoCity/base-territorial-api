package br.com.thermocity.baseterritorialapi.common;

import java.time.OffsetDateTime;
import java.util.List;

/** Corpo padrão das respostas de erro da API. */
public record ApiErro(int status, String erro, String mensagem, List<String> detalhes, String timestamp) {

    public static ApiErro de(int status, String erro, String mensagem, List<String> detalhes) {
        return new ApiErro(status, erro, mensagem, detalhes == null ? List.of() : detalhes,
                OffsetDateTime.now().toString());
    }
}
