package br.com.thermocity.baseterritorialapi.importacao;

import br.com.thermocity.baseterritorialapi.calor.GeoJsonController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;
import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Importação dos dados territoriais (CSV separado por ";", UTF-8), enviados como multipart no campo "arquivo".
 * Ordem: setores -> ipvs -> servicos -> calor.
 */
@RestController
@RequestMapping("/admin/importacoes")
public class ImportacaoController {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoController.class);

    private final ImportacaoService importacao;
    private final CargaInicial cargaInicial;
    private final GeoJsonController geoJson;

    public ImportacaoController(ImportacaoService importacao, CargaInicial cargaInicial, GeoJsonController geoJson) {
        this.importacao = importacao;
        this.cargaInicial = cargaInicial;
        this.geoJson = geoJson;
    }

    @PostMapping(value = "/setores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado setores(@RequestParam("arquivo") MultipartFile arquivo) {
        return importar("setores", arquivo, importacao::importarSetores);
    }

    @PostMapping(value = "/ipvs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado ipvs(@RequestParam("arquivo") MultipartFile arquivo) {
        return importar("ipvs", arquivo, importacao::importarIpvs);
    }

    @PostMapping(value = "/servicos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado servicos(@RequestParam("arquivo") MultipartFile arquivo) {
        return importar("servicos", arquivo, importacao::importarServicos);
    }

    @PostMapping(value = "/calor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado calor(@RequestParam("arquivo") MultipartFile arquivo) {
        return importar("calor", arquivo, importacao::importarCalor);
    }

    /** Recarrega os quatro arquivos que acompanham a aplicação (src/main/resources/dados). */
    @PostMapping("/padrao")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ImportacaoResultado> padrao() {
        log.info("Importação dos arquivos padrão (src/main/resources/dados) iniciada");
        List<ImportacaoResultado> resultados = cargaInicial.importarArquivosPadrao();
        geoJson.limparCache();
        resultados.forEach(r -> log.info("Importação padrão de {}: {} registros {}", r.tipo(), r.registros(), r.resumo()));
        return resultados;
    }

    private ImportacaoResultado importar(String tipo, MultipartFile arquivo,
                                         Function<InputStream, ImportacaoResultado> acao) {
        log.info("Importação de {} recebida: arquivo={} ({} bytes)", tipo,
                arquivo == null ? null : arquivo.getOriginalFilename(), arquivo == null ? 0 : arquivo.getSize());
        ImportacaoResultado r = acao.apply(abrir(arquivo));
        geoJson.limparCache(); // o mapa passa a refletir os dados novos
        log.info("Importação de {} concluída: {} registros {}", tipo, r.registros(), r.resumo());
        return r;
    }

    private InputStream abrir(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            log.warn("Importação recusada: nenhum arquivo no campo 'arquivo'");
            throw new RequisicaoInvalidaException("Envie um arquivo CSV no campo 'arquivo'");
        }
        try {
            return arquivo.getInputStream();
        } catch (IOException e) {
            log.error("Não foi possível ler o arquivo {}", arquivo.getOriginalFilename(), e);
            throw new RequisicaoInvalidaException("Não foi possível ler o arquivo enviado");
        }
    }
}
