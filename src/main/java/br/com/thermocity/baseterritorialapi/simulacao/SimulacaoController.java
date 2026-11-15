package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gêmeo digital: simulação de cenários de calor (Motor de Simulação). */
@RestController
@RequestMapping("/simulacoes")
public class SimulacaoController {

    private static final Logger log = LoggerFactory.getLogger(SimulacaoController.class);

    private final NivelCalorCalculadora calculadora;

    public SimulacaoController(NivelCalorCalculadora calculadora) {
        this.calculadora = calculadora;
    }

    /** Nível de calor de cada dia de um cenário, pelos limiares do banco (mesmas regras do modo real). */
    @PostMapping("/niveis")
    public NiveisSimulacaoResponse niveis(@Valid @RequestBody NiveisSimulacaoRequest req) {
        NivelCalorCalculadora.Resultado r = calculadora.classificar(req.icMaxPorDia(), req.icMaxDiasAnteriores());
        log.info("Níveis do cenário ({} dias, versão {}): {}", r.dias().size(), r.versaoLimiar(),
                r.dias().stream().map(d -> "NC" + d.nivel()).toList());
        return NiveisSimulacaoResponse.de(r, req.dataInicio());
    }
}
