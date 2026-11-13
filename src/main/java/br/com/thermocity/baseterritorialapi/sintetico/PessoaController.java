package br.com.thermocity.baseterritorialapi.sintetico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.thermocity.baseterritorialapi.common.RecursoNaoEncontradoException;
import br.com.thermocity.baseterritorialapi.setor.SetorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta das pessoas fictícias. */
@RestController
public class PessoaController {

    private static final Logger log = LoggerFactory.getLogger(PessoaController.class);

    private final PessoaSinteticaRepository pessoas;
    private final SetorService setores;

    public PessoaController(PessoaSinteticaRepository pessoas, SetorService setores) {
        this.pessoas = pessoas;
        this.setores = setores;
    }

    @GetMapping("/pessoas/{id}")
    public PessoaResponse buscar(@PathVariable Long id) {
        log.info("Buscando pessoa {}", id);
        return pessoas.findById(id).map(PessoaResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
    }

    @GetMapping("/pessoas")
    public List<PessoaResponse> doSetor(@RequestParam String setor) {
        setores.buscar(setor); // 404 se o setor não existir
        List<PessoaResponse> lista = pessoas.findBySetorCodigoOrderByIdAsc(setor).stream()
                .map(PessoaResponse::de).toList();
        log.info("Pessoas do setor {}: {} encontradas", setor, lista.size());
        return lista;
    }
}
