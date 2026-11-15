package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Gêmeo digital: simulação de cenários de calor (Motor de Simulação). */
@RestController
@RequestMapping("/simulacoes")
public class SimulacaoController {

    private static final Logger log = LoggerFactory.getLogger(SimulacaoController.class);

    private final NivelCalorCalculadora calculadora;
    private final ModeloOndaCalorService modelos;

    public SimulacaoController(NivelCalorCalculadora calculadora, ModeloOndaCalorService modelos) {
        this.calculadora = calculadora;
        this.modelos = modelos;
    }

    /**
     * Ondas de calor reais da série A707 (episódios NC4/NC5 com pelo menos duracaoMinima dias seguidos)
     * e as mesmas ondas com acréscimo no IC. A série histórica muda pouco, então a resposta pode ficar em cache.
     */
    @GetMapping("/modelos")
    public ResponseEntity<ModelosOndaCalorResponse> modelos(
            @RequestParam(defaultValue = "1,3") List<Double> acrescimos,
            @RequestParam(defaultValue = "3") int duracaoMinima) {
        ModelosOndaCalorResponse r = modelos.listar(acrescimos, duracaoMinima);
        log.info("Modelos de onda de calor: {} (acréscimos {}, duração mínima {} dias, máximo histórico {} °C)",
                r.modelos().size(), acrescimos, duracaoMinima, r.maxHistoricoC());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(r);
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
