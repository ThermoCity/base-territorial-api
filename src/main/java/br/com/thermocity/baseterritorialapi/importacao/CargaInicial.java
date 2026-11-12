package br.com.thermocity.baseterritorialapi.importacao;

import br.com.thermocity.baseterritorialapi.calor.CalorSetorRepository;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Ao subir a aplicação, com a carga habilitada: se o banco não tiver setores, importa os CSVs de
 * src/main/resources/dados; se já tiver setores mas ainda não tiver calor, importa só o calor.csv.
 * Em seguida monta o índice espacial em memória.
 */
@Component
public class CargaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final ImportacaoService importacao;
    private final SetorRepository setores;
    private final CalorSetorRepository calor;
    private final GeoService geo;
    private final boolean habilitada;

    public CargaInicial(ImportacaoService importacao, SetorRepository setores, CalorSetorRepository calor,
                        GeoService geo, @Value("${thermocity.carga-inicial.habilitada:false}") boolean habilitada) {
        this.importacao = importacao;
        this.setores = setores;
        this.calor = calor;
        this.geo = geo;
        this.habilitada = habilitada;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (habilitada && setores.count() == 0) {
            importarArquivosPadrao().forEach(this::registrar);
        } else if (habilitada && calor.count() == 0) {
            registrar(importacao.importarCalor(abrir("dados/calor.csv")));
        }
        geo.recarregar(setores.findAll());
    }

    public List<ImportacaoResultado> importarArquivosPadrao() {
        return List.of(
                importacao.importarSetores(abrir("dados/setores.csv")),
                importacao.importarIpvs(abrir("dados/ipvs.csv")),
                importacao.importarServicos(abrir("dados/servicos.csv")),
                importacao.importarCalor(abrir("dados/calor.csv")));
    }

    private void registrar(ImportacaoResultado r) {
        log.info("Carga inicial: {} -> {} registros {}", r.tipo(), r.registros(), r.resumo());
    }

    private InputStream abrir(String caminho) {
        try {
            return new ClassPathResource(caminho).getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Arquivo de carga não encontrado: " + caminho, e);
        }
    }
}
