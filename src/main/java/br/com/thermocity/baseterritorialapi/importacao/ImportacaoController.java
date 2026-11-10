package br.com.thermocity.baseterritorialapi.importacao;

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

    private final ImportacaoService importacao;
    private final CargaInicial cargaInicial;

    public ImportacaoController(ImportacaoService importacao, CargaInicial cargaInicial) {
        this.importacao = importacao;
        this.cargaInicial = cargaInicial;
    }

    @PostMapping(value = "/setores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado setores(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacao.importarSetores(abrir(arquivo));
    }

    @PostMapping(value = "/ipvs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado ipvs(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacao.importarIpvs(abrir(arquivo));
    }

    @PostMapping(value = "/servicos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado servicos(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacao.importarServicos(abrir(arquivo));
    }

    @PostMapping(value = "/calor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResultado calor(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacao.importarCalor(abrir(arquivo));
    }

    /** Recarrega os quatro arquivos que acompanham a aplicação (src/main/resources/dados). */
    @PostMapping("/padrao")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ImportacaoResultado> padrao() {
        return cargaInicial.importarArquivosPadrao();
    }

    private InputStream abrir(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie um arquivo CSV no campo 'arquivo'");
        }
        try {
            return arquivo.getInputStream();
        } catch (IOException e) {
            throw new RequisicaoInvalidaException("Não foi possível ler o arquivo enviado");
        }
    }
}
