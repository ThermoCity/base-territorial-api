package br.com.thermocity.baseterritorialapi.setor;

import br.com.thermocity.baseterritorialapi.common.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SetorService {

    private final SetorRepository repositorio;

    public SetorService(SetorRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public Setor buscar(String codigo) {
        return repositorio.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Setor> listar() {
        return repositorio.findAll().stream().sorted(Comparator.comparing(Setor::getCodigo)).toList();
    }
}
