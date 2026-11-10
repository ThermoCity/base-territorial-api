package br.com.thermocity.baseterritorialapi.sintetico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** PBI #6: gera o cadastro sintético (substitui o anterior). */
@RestController
public class SinteticoController {

    private final GeradorSinteticoService gerador;

    public SinteticoController(GeradorSinteticoService gerador) {
        this.gerador = gerador;
    }

    @PostMapping("/admin/sinteticos")
    @ResponseStatus(HttpStatus.CREATED)
    public GeracaoResultado gerar(@Valid @RequestBody GerarSinteticosRequest req) {
        return gerador.gerar(req.quantidade(), req.semente());
    }
}
