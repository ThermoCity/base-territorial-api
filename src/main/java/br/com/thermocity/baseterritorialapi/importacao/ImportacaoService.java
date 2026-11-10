package br.com.thermocity.baseterritorialapi.importacao;

import br.com.thermocity.baseterritorialapi.calor.CalorSetor;
import br.com.thermocity.baseterritorialapi.calor.CalorSetorRepository;
import br.com.thermocity.baseterritorialapi.common.ImportacaoInvalidaException;
import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import br.com.thermocity.baseterritorialapi.csv.CsvLeitor;
import br.com.thermocity.baseterritorialapi.csv.CsvLeitor.Linha;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.servico.Servico;
import br.com.thermocity.baseterritorialapi.servico.ServicoRepository;
import br.com.thermocity.baseterritorialapi.servico.StatusCoordenada;
import br.com.thermocity.baseterritorialapi.servico.TipoServico;
import br.com.thermocity.baseterritorialapi.setor.Setor;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import br.com.thermocity.baseterritorialapi.setor.StatusDado;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importa os CSVs gerados a partir do GeoPackage (QGIS).
 * Regra geral: o arquivo inteiro é validado antes de gravar. Se houver qualquer erro, nada é gravado (HTTP 422).
 */
@Service
public class ImportacaoService {

    private static final Set<String> REVISOES = Set.of("manter", "potencial", "revisar", "remover", "publico_alvo");

    private final SetorRepository setores;
    private final ServicoRepository servicos;
    private final GeoService geo;
    private final CalorSetorRepository calor;

    public ImportacaoService(SetorRepository setores, ServicoRepository servicos, GeoService geo,
                             CalorSetorRepository calor) {
        this.setores = setores;
        this.servicos = servicos;
        this.geo = geo;
        this.calor = calor;
    }

    // ------------------------------------------------------------------ setores (PBI #3)

    @Transactional
    public ImportacaoResultado importarSetores(InputStream arquivo) {
        List<Linha> linhas = ler(arquivo, List.of("codigo", "pop_total", "pop_60_69", "pop_70mais", "dado_censo", "geometria_wkt"));
        List<String> erros = new ArrayList<>();
        List<Setor> novos = new ArrayList<>();
        OffsetDateTime agora = OffsetDateTime.now();

        for (Linha l : linhas) {
            String codigo = l.valor("codigo");
            if (codigo == null) {
                erros.add("Linha " + l.numero() + ": código do setor ausente");
                continue;
            }
            if (!codigo.matches("\\d{15}")) {
                erros.add("Linha " + l.numero() + ": código do setor deve ter 15 dígitos (" + codigo + ")");
                continue;
            }
            Geometry geometria;
            try {
                geometria = geo.lerWkt(l.valor("geometria_wkt"));
            } catch (ParseException | NullPointerException e) {
                erros.add("Linha " + l.numero() + ": geometria inválida ou ausente");
                continue;
            }
            Setor s = setores.findById(codigo).orElseGet(Setor::new);
            s.setCodigo(codigo);
            s.setSituacao(l.valor("situacao"));
            s.setCdSituacao(l.valor("cd_situacao"));
            s.setCdTipo(l.valor("cd_tipo"));
            s.setAreaKm2(decimal(l, "area_km2", erros));
            s.setCdMunicipio(l.valor("cd_municipio"));
            s.setNmMunicipio(l.valor("nm_municipio"));
            s.setCdDistrito(l.valor("cd_distrito"));
            s.setNmDistrito(l.valor("nm_distrito"));
            s.setCdSubdistrito(l.valor("cd_subdistrito"));
            s.setNmSubdistrito(l.valor("nm_subdistrito"));
            s.setCdBairro(l.valor("cd_bairro"));
            s.setNmBairro(l.valor("nm_bairro"));
            s.setPopTotal(inteiro(l, "pop_total", erros));
            s.setPop60a69(inteiro(l, "pop_60_69", erros));
            s.setPop70mais(inteiro(l, "pop_70mais", erros));
            s.setStatusCenso(StatusDado.deTexto(l.valor("dado_censo")));
            double[] ref = geo.pontoInterior(geometria);
            s.setLatReferencia(ref[0]);
            s.setLonReferencia(ref[1]);
            s.setGeometriaWkt(l.valor("geometria_wkt"));
            s.setAtualizadoEm(agora);
            novos.add(s);
        }
        exigirSemErros(erros, linhas.size());
        setores.saveAll(novos);
        geo.recarregar(setores.findAll());
        return new ImportacaoResultado("setores", novos.size(), contar(novos, x -> String.valueOf(x.getStatusCenso())));
    }

    // ------------------------------------------------------------------ IPVS (PBI #4)

    @Transactional
    public ImportacaoResultado importarIpvs(InputStream arquivo) {
        List<Linha> linhas = ler(arquivo, List.of("codigo_setor", "grupo", "nome", "edicao"));
        List<String> erros = new ArrayList<>();
        List<Setor> alterados = new ArrayList<>();

        for (Linha l : linhas) {
            String codigo = l.valor("codigo_setor");
            if (codigo == null) {
                erros.add("Linha " + l.numero() + ": código do setor ausente");
                continue;
            }
            Setor s = setores.findById(codigo).orElse(null);
            if (s == null) {
                erros.add("Linha " + l.numero() + ": setor inexistente (" + codigo + ")");
                continue;
            }
            Integer grupo = inteiro(l, "grupo", erros);
            if (grupo != null && (grupo < 1 || grupo > 6)) {
                erros.add("Linha " + l.numero() + ": grupo de vulnerabilidade deve estar entre 1 e 6 (recebido " + grupo + ")");
                continue;
            }
            s.setIpvsGrupo(grupo);
            s.setIpvsNome(l.valor("nome"));
            s.setIpvsEdicao(l.valor("edicao"));
            s.setIpvsStatus(grupo == null ? StatusDado.SEM_DADO : StatusDado.OK);
            alterados.add(s);
        }
        exigirSemErros(erros, linhas.size());
        setores.saveAll(alterados);
        return new ImportacaoResultado("ipvs", alterados.size(), contar(alterados, x -> String.valueOf(x.getIpvsStatus())));
    }

    // ------------------------------------------------------------------ UBS e refúgios (PBI #5)

    @Transactional
    public ImportacaoResultado importarServicos(InputStream arquivo) {
        List<Linha> linhas = ler(arquivo, List.of("id", "nome", "tipo", "lat", "lon"));
        List<String> erros = new ArrayList<>();
        List<Servico> novos = new ArrayList<>();

        for (Linha l : linhas) {
            String id = l.valor("id");
            if (id == null) {
                erros.add("Linha " + l.numero() + ": id ausente");
                continue;
            }
            TipoServico tipo;
            try {
                tipo = TipoServico.valueOf(String.valueOf(l.valor("tipo")).toUpperCase());
            } catch (IllegalArgumentException e) {
                erros.add("Linha " + l.numero() + ": tipo deve ser UBS ou REFUGIO");
                continue;
            }
            String revisao = l.valor("revisao");
            if (revisao != null && !REVISOES.contains(revisao)) {
                erros.add("Linha " + l.numero() + ": revisão inválida (" + revisao + ")");
                continue;
            }
            Servico s = new Servico();
            s.setId(id);
            s.setNome(l.valor("nome"));
            s.setTipo(tipo);
            s.setCategoria(l.valor("categoria"));
            s.setEndereco(l.valor("endereco"));
            s.setBairro(l.valor("bairro"));
            s.setFonte(l.valor("fonte"));
            s.setIdFonte(l.valor("id_fonte"));
            s.setRevisao(tipo == TipoServico.REFUGIO ? revisao : null);
            s.setMotivoRevisao(l.valor("motivo_revisao"));
            Double lat = decimal(l, "lat", erros);
            Double lon = decimal(l, "lon", erros);
            s.setLat(lat);
            s.setLon(lon);
            if (lat == null || lon == null) {
                s.setCoordStatus(StatusCoordenada.SEM_COORDENADA);
            } else {
                var setor = geo.setorDoPonto(lat, lon);
                s.setSetorCodigo(setor.orElse(null));
                s.setCoordStatus(setor.isPresent() ? StatusCoordenada.OK : StatusCoordenada.FORA_MUNICIPIO);
            }
            novos.add(s);
        }
        exigirSemErros(erros, linhas.size());
        servicos.saveAll(novos);
        return new ImportacaoResultado("servicos", novos.size(),
                contar(novos, x -> x.getTipo() + (x.getRevisao() == null ? "" : ":" + x.getRevisao())));
    }

    // ------------------------------------------------------------------ calor por setor (PBI #11)

    /**
     * Indicadores de calor por setor (cena Landsat de verão processada no QGIS).
     * Linha sem temperatura de superfície = setor sem pixel válido (nuvem): grava com status SEM_DADO.
     */
    @Transactional
    public ImportacaoResultado importarCalor(InputStream arquivo) {
        List<Linha> linhas = ler(arquivo, List.of("codigo_setor", "lst_verao_c", "classe_calor", "cena_landsat", "data_cena"));
        List<String> erros = new ArrayList<>();
        List<CalorSetor> novos = new ArrayList<>();
        OffsetDateTime agora = OffsetDateTime.now();

        for (Linha l : linhas) {
            String codigo = l.valor("codigo_setor");
            if (codigo == null) {
                erros.add("Linha " + l.numero() + ": código do setor ausente");
                continue;
            }
            if (!setores.existsById(codigo)) {
                erros.add("Linha " + l.numero() + ": setor inexistente (" + codigo + ")");
                continue;
            }
            Double lst = decimal(l, "lst_verao_c", erros);
            Integer classe = inteiro(l, "classe_calor", erros);
            if (lst != null && classe == null) {
                erros.add("Linha " + l.numero() + ": classe de calor ausente para setor com temperatura");
                continue;
            }
            if (classe != null && (classe < 1 || classe > 5)) {
                erros.add("Linha " + l.numero() + ": classe de calor deve estar entre 1 e 5 (recebido " + classe + ")");
                continue;
            }
            LocalDate dataCena;
            try {
                dataCena = l.valor("data_cena") == null ? null : LocalDate.parse(l.valor("data_cena"));
            } catch (DateTimeParseException e) {
                erros.add("Linha " + l.numero() + ": data da cena deve estar no formato AAAA-MM-DD (" + l.valor("data_cena") + ")");
                continue;
            }
            CalorSetor c = calor.findById(codigo).orElseGet(CalorSetor::new);
            c.setCodigoSetor(codigo);
            c.setLstVeraoC(lst);
            c.setNdviMedio(decimal(l, "ndvi_medio", erros));
            c.setPctConstrucoes(decimal(l, "pct_construcoes", erros));
            c.setPctVegetacaoDensa(decimal(l, "pct_vegetacao_densa", erros));
            c.setTaEstimadaC(decimal(l, "ta_estimada_c", erros));
            c.setClasseCalor(lst == null ? null : classe);
            c.setPixelsValidos(inteiro(l, "pixels_validos", erros));
            c.setCenaLandsat(l.valor("cena_landsat"));
            c.setDataCena(dataCena);
            c.setStatus(lst == null ? StatusDado.SEM_DADO : StatusDado.OK);
            c.setAtualizadoEm(agora);
            novos.add(c);
        }
        exigirSemErros(erros, linhas.size());
        calor.saveAll(novos);
        return new ImportacaoResultado("calor", novos.size(),
                contar(novos, x -> x.getStatus() == StatusDado.OK ? "classe_" + x.getClasseCalor() : String.valueOf(x.getStatus())));
    }

    // ------------------------------------------------------------------ apoio

    private List<Linha> ler(InputStream arquivo, List<String> obrigatorias) {
        try {
            return CsvLeitor.ler(arquivo, obrigatorias);
        } catch (IOException e) {
            throw new RequisicaoInvalidaException("Arquivo CSV inválido: " + e.getMessage());
        }
    }

    private void exigirSemErros(List<String> erros, int totalLinhas) {
        if (totalLinhas == 0) {
            throw new RequisicaoInvalidaException("Arquivo sem registros");
        }
        if (!erros.isEmpty()) {
            throw new ImportacaoInvalidaException(
                    erros.size() + " registro(s) inválido(s). Nenhum dado do arquivo foi gravado.", erros);
        }
    }

    private Integer inteiro(Linha l, String coluna, List<String> erros) {
        String v = l.valor(coluna);
        if (v == null) return null;
        try {
            return (int) Double.parseDouble(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            erros.add("Linha " + l.numero() + ": valor não numérico em " + coluna + " (" + v + ")");
            return null;
        }
    }

    private Double decimal(Linha l, String coluna, List<String> erros) {
        String v = l.valor(coluna);
        if (v == null) return null;
        try {
            return Double.parseDouble(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            erros.add("Linha " + l.numero() + ": valor não numérico em " + coluna + " (" + v + ")");
            return null;
        }
    }

    private static <T> Map<String, Long> contar(List<T> itens, Function<T, String> chave) {
        return itens.stream().collect(Collectors.groupingBy(chave, java.util.TreeMap::new, Collectors.counting()));
    }
}
