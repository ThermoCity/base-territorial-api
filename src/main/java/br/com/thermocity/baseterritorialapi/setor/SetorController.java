package br.com.thermocity.baseterritorialapi.setor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.thermocity.baseterritorialapi.calor.CalorSetor;
import br.com.thermocity.baseterritorialapi.calor.CalorSetorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** PBIs #3, #4 e #11: consulta de setores com população (Censo 2022), IPVS e calor. */
@RestController
@RequestMapping("/setores")
public class SetorController {

    private static final Logger log = LoggerFactory.getLogger(SetorController.class);

    private final SetorService servico;
    private final CalorSetorRepository calor;

    public SetorController(SetorService servico, CalorSetorRepository calor) {
        this.servico = servico;
        this.calor = calor;
    }

    @GetMapping
    public List<SetorResponse> listar() {
        Map<String, CalorSetor> porSetor = calor.findAll().stream()
                .collect(Collectors.toMap(CalorSetor::getCodigoSetor, Function.identity()));
        List<SetorResponse> setores = servico.listar().stream()
                .map(s -> SetorResponse.de(s, porSetor.get(s.getCodigo()))).toList();
        log.info("Listagem de setores: {} setores, {} com dado de calor", setores.size(), porSetor.size());
        return setores;
    }

    @GetMapping("/{codigo}")
    public SetorResponse buscar(@PathVariable String codigo) {
        log.info("Buscando setor {}", codigo);
        Setor s = servico.buscar(codigo);
        CalorSetor c = calor.findById(codigo).orElse(null);
        log.info("Setor {} encontrado (classe de calor: {})", codigo, c == null ? "sem dado" : c.getClasseCalor());
        return SetorResponse.de(s, c);
    }
}
