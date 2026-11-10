package br.com.thermocity.baseterritorialapi.setor;

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
        return servico.listar().stream().map(s -> SetorResponse.de(s, porSetor.get(s.getCodigo()))).toList();
    }

    @GetMapping("/{codigo}")
    public SetorResponse buscar(@PathVariable String codigo) {
        Setor s = servico.buscar(codigo);
        return SetorResponse.de(s, calor.findById(codigo).orElse(null));
    }
}
