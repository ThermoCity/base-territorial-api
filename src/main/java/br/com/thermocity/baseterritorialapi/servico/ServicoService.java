package br.com.thermocity.baseterritorialapi.servico;

import br.com.thermocity.baseterritorialapi.common.RecursoNaoEncontradoException;
import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.setor.Setor;
import br.com.thermocity.baseterritorialapi.setor.SetorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ServicoService {

    /** Refúgios confirmados como públicos. */
    public static final Set<String> REFUGIO_CONFIRMADO = Set.of("manter");
    /** Refúgios confirmados + os que dependem de parceria. */
    public static final Set<String> REFUGIO_COM_POTENCIAIS = Set.of("manter", "potencial");

    private final ServicoRepository repositorio;
    private final SetorService setores;
    private final GeoService geo;

    public ServicoService(ServicoRepository repositorio, SetorService setores, GeoService geo) {
        this.repositorio = repositorio;
        this.setores = setores;
        this.geo = geo;
    }

    /** GET /setores/{codigo}/servicos */
    @Transactional(readOnly = true)
    public ServicosDoSetorResponse servicosDoSetor(String codigoSetor, int raioMetros) {
        if (raioMetros <= 0 || raioMetros > 20_000) {
            throw new RequisicaoInvalidaException("O raio deve estar entre 1 e 20000 metros");
        }
        Setor setor = setores.buscar(codigoSetor);
        double lat = setor.getLatReferencia();
        double lon = setor.getLonReferencia();

        List<Servico> ubs = repositorio.findByTipoAndCoordStatus(TipoServico.UBS, StatusCoordenada.OK);
        List<Servico> refugios = refugios(REFUGIO_COM_POTENCIAIS);

        ServicoResponse ubsProxima = maisProximo(ubs, lat, lon).orElse(null);
        ServicoResponse refugioProximo = maisProximo(refugios(REFUGIO_CONFIRMADO), lat, lon).orElse(null);

        List<ServicoResponse> noRaio = java.util.stream.Stream.concat(ubs.stream(), refugios.stream())
                .map(s -> ServicoResponse.de(s, GeoService.distanciaMetros(lat, lon, s.getLat(), s.getLon())))
                .filter(r -> r.distanciaMetros() <= raioMetros)
                .sorted(Comparator.comparing(ServicoResponse::distanciaMetros))
                .toList();

        return new ServicosDoSetorResponse(codigoSetor, ubsProxima, refugioProximo, raioMetros, noRaio);
    }

    /** GET /refugios/proximo */
    @Transactional(readOnly = true)
    public ServicoResponse refugioMaisProximo(double lat, double lon, boolean incluirPotenciais) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180 || !geo.dentroDoMunicipio(lat, lon)) {
            throw new RequisicaoInvalidaException("Coordenada fora da área atendida");
        }
        return maisProximo(refugios(incluirPotenciais ? REFUGIO_COM_POTENCIAIS : REFUGIO_CONFIRMADO), lat, lon)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum refúgio cadastrado"));
    }

    /** UBS mais próxima de um ponto (usada também pelo cadastro sintético). */
    @Transactional(readOnly = true)
    public Optional<ServicoResponse> ubsMaisProxima(double lat, double lon) {
        return maisProximo(repositorio.findByTipoAndCoordStatus(TipoServico.UBS, StatusCoordenada.OK), lat, lon);
    }

    /** GET /servicos: todas as UBS e os refúgios sugeríveis (manter e potencial) com coordenada, para o mapa. */
    @Transactional(readOnly = true)
    public List<ServicoResponse> listarParaMapa() {
        return java.util.stream.Stream.concat(listarUbs().stream(), refugios(REFUGIO_COM_POTENCIAIS).stream())
                .filter(Servico::temCoordenada)
                .sorted(Comparator.comparing(Servico::getId))
                .map(s -> ServicoResponse.de(s, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Servico> listarUbs() {
        return repositorio.findByTipoAndCoordStatus(TipoServico.UBS, StatusCoordenada.OK);
    }

    /** Refúgios com coordenada: só os públicos confirmados ou também os potenciais (motor de simulação). */
    @Transactional(readOnly = true)
    public List<Servico> refugiosComCoordenada(boolean incluirPotenciais) {
        return refugios(incluirPotenciais ? REFUGIO_COM_POTENCIAIS : REFUGIO_CONFIRMADO).stream()
                .filter(Servico::temCoordenada)
                .toList();
    }

    private List<Servico> refugios(Set<String> revisoesAceitas) {
        return repositorio.findByTipoAndCoordStatus(TipoServico.REFUGIO, StatusCoordenada.OK).stream()
                .filter(s -> s.getRevisao() != null && revisoesAceitas.contains(s.getRevisao()))
                .toList();
    }

    private Optional<ServicoResponse> maisProximo(List<Servico> lista, double lat, double lon) {
        return lista.stream()
                .filter(Servico::temCoordenada)
                .map(s -> ServicoResponse.de(s, GeoService.distanciaMetros(lat, lon, s.getLat(), s.getLon())))
                .min(Comparator.comparing(ServicoResponse::distanciaMetros));
    }
}
