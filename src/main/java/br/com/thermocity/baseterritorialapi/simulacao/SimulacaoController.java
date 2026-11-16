package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.common.PaginaResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Gêmeo digital: simulação de cenários de calor (Motor de Simulação). */
@RestController
@RequestMapping("/simulacoes")
public class SimulacaoController {

    private static final Logger log = LoggerFactory.getLogger(SimulacaoController.class);

    private final NivelCalorCalculadora calculadora;
    private final ModeloOndaCalorService modelos;
    private final SimulacaoService simulacoes;

    public SimulacaoController(NivelCalorCalculadora calculadora, ModeloOndaCalorService modelos,
                               SimulacaoService simulacoes) {
        this.calculadora = calculadora;
        this.modelos = modelos;
        this.simulacoes = simulacoes;
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

    /** Cria e roda um cenário. 201 com o id; o resultado fica em GET /simulacoes/{id}. */
    @PostMapping
    public ResponseEntity<SimulacaoResumoResponse> criar(@Valid @RequestBody CriarSimulacaoRequest req) {
        Simulacao s = simulacoes.criar(req);
        log.info("Simulação {} criada: {} dias, NC máximo {}, {} dia(s) de extrapolação",
                s.getId(), s.getQtdDias(), s.getNivelMaximo(), s.getDiasExtrapolados());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(s.getId()).toUri())
                .body(SimulacaoResumoResponse.de(s));
    }

    /** Simulações já feitas, das mais recentes para as mais antigas (page, size). */
    @GetMapping
    public PaginaResponse<SimulacaoResumoResponse> listar(
            @PageableDefault(size = 20, sort = {"criadoEm", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return simulacoes.listar(pageable);
    }

    /** Cenário e resultado por dia e por setor. Com ?dia=N, só os setores daquele dia. */
    @GetMapping("/{id}")
    public SimulacaoDetalheResponse detalhar(@PathVariable Long id, @RequestParam(required = false) Integer dia) {
        return simulacoes.detalhar(id, dia);
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
