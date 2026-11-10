package br.com.thermocity.baseterritorialapi.geo;

import br.com.thermocity.baseterritorialapi.setor.Setor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Mantém em memória os polígonos dos setores (420 em Presidente Prudente) para responder
 * "em qual setor está este ponto?" sem depender de extensão espacial no banco.
 * Coordenadas em graus (SIRGAS 2000, EPSG:4674): x = longitude, y = latitude.
 */
@Service
public class GeoService {

    private static final double RAIO_TERRA_M = 6_371_008.8;

    private final GeometryFactory fabrica = new GeometryFactory(new PrecisionModel(), 4674);

    private volatile STRtree indice = new STRtree();
    private volatile Map<String, Geometry> geometrias = Map.of();

    /** Reconstrói o índice espacial a partir dos setores gravados. */
    public synchronized void recarregar(List<Setor> setores) {
        STRtree novoIndice = new STRtree();
        Map<String, Geometry> novas = new HashMap<>();
        for (Setor setor : setores) {
            if (setor.getGeometriaWkt() == null) continue;
            try {
                Geometry g = lerWkt(setor.getGeometriaWkt());
                novas.put(setor.getCodigo(), g);
                novoIndice.insert(g.getEnvelopeInternal(),
                        new Entrada(setor.getCodigo(), PreparedGeometryFactory.prepare(g)));
            } catch (ParseException e) {
                // geometria inválida já é barrada na importação; aqui apenas ignora
            }
        }
        novoIndice.build();
        this.geometrias = Map.copyOf(novas);
        this.indice = novoIndice;
    }

    public Geometry lerWkt(String wkt) throws ParseException {
        Geometry g = new WKTReader(fabrica).read(wkt);
        if (g.isEmpty()) throw new ParseException("Geometria vazia");
        return g;
    }

    /** Código do setor que contém o ponto, se o ponto estiver dentro do município. */
    public Optional<String> setorDoPonto(double lat, double lon) {
        Point p = fabrica.createPoint(new Coordinate(lon, lat));
        @SuppressWarnings("unchecked")
        List<Entrada> candidatos = indice.query(new Envelope(lon, lon, lat, lat));
        for (Entrada e : candidatos) {
            if (e.geometria().covers(p)) return Optional.of(e.codigo());
        }
        return Optional.empty();
    }

    public boolean dentroDoMunicipio(double lat, double lon) {
        return setorDoPonto(lat, lon).isPresent();
    }

    /** Ponto garantidamente dentro do polígono: [lat, lon]. */
    public double[] pontoInterior(Geometry g) {
        Coordinate c = g.getInteriorPoint().getCoordinate();
        return new double[]{c.y, c.x};
    }

    /** Ponto aleatório dentro do setor: [lat, lon]. Se não achar em 200 tentativas, usa o ponto interior. */
    public double[] pontoAleatorio(String codigoSetor, RandomGenerator aleatorio) {
        Geometry g = geometrias.get(codigoSetor);
        if (g == null) throw new IllegalArgumentException("Setor sem geometria: " + codigoSetor);
        Envelope env = g.getEnvelopeInternal();
        PreparedGeometry pg = PreparedGeometryFactory.prepare(g);
        for (int i = 0; i < 200; i++) {
            double lon = env.getMinX() + aleatorio.nextDouble() * env.getWidth();
            double lat = env.getMinY() + aleatorio.nextDouble() * env.getHeight();
            if (pg.covers(fabrica.createPoint(new Coordinate(lon, lat)))) return new double[]{lat, lon};
        }
        return pontoInterior(g);
    }

    /** Distância em linha reta (fórmula de haversine), em metros. */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.sqrt(a));
    }

    private record Entrada(String codigo, PreparedGeometry geometria) {
    }
}
