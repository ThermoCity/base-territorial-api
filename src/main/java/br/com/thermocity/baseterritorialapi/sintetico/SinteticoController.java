package br.com.thermocity.baseterritorialapi.sintetico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** PBI #6: gera o cadastro sintético (substitui o anterior). */
@RestController
public class SinteticoController {

    private static final Logger log = LoggerFactory.getLogger(SinteticoController.class);

    private final GeradorSinteticoService gerador;

    public SinteticoController(GeradorSinteticoService gerador) {
        this.gerador = gerador;
    }

    @PostMapping("/admin/sinteticos")
    @ResponseStatus(HttpStatus.CREATED)
    public GeracaoResultado gerar(@Valid @RequestBody GerarSinteticosRequest req) {
        log.info("Geração de pessoas sintéticas iniciada: quantidade={}, semente={}", req.quantidade(), req.semente());
        GeracaoResultado r = gerador.gerar(req.quantidade(), req.semente());
        log.info("Geração concluída: {} pessoas em {} setores (semente {}), faixas={}, sexo={}",
                r.quantidade(), r.setoresComPessoas(), r.semente(), r.porFaixaEtaria(), r.porSexo());
        return r;
    }
}
