package br.com.thermocity.baseterritorialapi.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** PBI #5: UBS e possíveis refúgios. */
@RestController
public class ServicoController {

    private static final Logger log = LoggerFactory.getLogger(ServicoController.class);

    private final ServicoService servico;

    public ServicoController(ServicoService servico) {
        this.servico = servico;
    }

    /** Todas as UBS e os refúgios sugeríveis, para desenhar no mapa do painel. */
    @GetMapping("/servicos")
    public org.springframework.http.ResponseEntity<java.util.List<ServicoResponse>> todos() {
        java.util.List<ServicoResponse> r = servico.listarParaMapa();
        log.info("Serviços para o mapa: {}", r.size());
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS).cachePublic())
                .body(r);
    }

    /** UBS e refúgios que atendem o setor, com distância em metros. */
    @GetMapping("/setores/{codigo}/servicos")
    public ServicosDoSetorResponse doSetor(@PathVariable String codigo,
                                           @RequestParam(defaultValue = "1500") int raioMetros) {
        log.info("Serviços do setor {} (raio {} m)", codigo, raioMetros);
        ServicosDoSetorResponse r = servico.servicosDoSetor(codigo, raioMetros);
        log.info("Setor {}: UBS mais próxima={}, refúgio mais próximo={}, {} serviços no raio", codigo,
                resumo(r.ubsMaisProxima()), resumo(r.refugioMaisProximo()),
                r.servicosNoRaio() == null ? 0 : r.servicosNoRaio().size());
        return r;
    }

    /** Refúgio mais próximo de uma coordenada. Por padrão só refúgios públicos confirmados. */
    @GetMapping("/refugios/proximo")
    public ServicoResponse refugioMaisProximo(@RequestParam double lat, @RequestParam double lon,
                                              @RequestParam(defaultValue = "false") boolean incluirPotenciais) {
        log.info("Refúgio mais próximo de ({}, {}), incluirPotenciais={}", lat, lon, incluirPotenciais);
        ServicoResponse r = servico.refugioMaisProximo(lat, lon, incluirPotenciais);
        log.info("Refúgio encontrado: {}", resumo(r));
        return r;
    }

    private static String resumo(ServicoResponse s) {
        return s == null ? "nenhum" : s.id() + " (" + s.distanciaMetros() + " m)";
    }
}
