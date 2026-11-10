package br.com.thermocity.baseterritorialapi.servico;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** PBI #5: UBS e possíveis refúgios. */
@RestController
public class ServicoController {

    private final ServicoService servico;

    public ServicoController(ServicoService servico) {
        this.servico = servico;
    }

    /** UBS e refúgios que atendem o setor, com distância em metros. */
    @GetMapping("/setores/{codigo}/servicos")
    public ServicosDoSetorResponse doSetor(@PathVariable String codigo,
                                           @RequestParam(defaultValue = "1500") int raioMetros) {
        return servico.servicosDoSetor(codigo, raioMetros);
    }

    /** Refúgio mais próximo de uma coordenada. Por padrão só refúgios públicos confirmados. */
    @GetMapping("/refugios/proximo")
    public ServicoResponse refugioMaisProximo(@RequestParam double lat, @RequestParam double lon,
                                              @RequestParam(defaultValue = "false") boolean incluirPotenciais) {
        return servico.refugioMaisProximo(lat, lon, incluirPotenciais);
    }
}
