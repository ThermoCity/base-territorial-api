package br.com.thermocity.baseterritorialapi.calor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.setor.Setor;
import br.com.thermocity.baseterritorialapi.setor.SetorService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polígonos dos setores em GeoJSON, com população 60+, IPVS e classe de calor como propriedades,
 * para o painel pintar o mapa (camada ONDE do gêmeo digital).
 */
@RestController
public class GeoJsonController {

    private static final Logger log = LoggerFactory.getLogger(GeoJsonController.class);

    private final SetorService setores;
    private final CalorSetorRepository calor;
    private final GeoService geo;

    public GeoJsonController(SetorService setores, CalorSetorRepository calor, GeoService geo) {
        this.setores = setores;
        this.calor = calor;
        this.geo = geo;
    }

    @GetMapping(value = "/setores/geojson", produces = "application/geo+json")
    public Map<String, Object> geojson() {
        Map<String, CalorSetor> porSetor = new LinkedHashMap<>();
        calor.findAll().forEach(c -> porSetor.put(c.getCodigoSetor(), c));

        List<Map<String, Object>> features = new ArrayList<>();
        int semGeometria = 0;
        for (Setor s : setores.listar()) {
            if (s.getGeometriaWkt() == null) {
                semGeometria++;
                continue;
            }
            Geometry g;
            try {
                g = geo.lerWkt(s.getGeometriaWkt());
            } catch (ParseException e) {
                log.warn("Setor {} com WKT inválido, ficou fora do GeoJSON: {}", s.getCodigo(), e.getMessage());
                semGeometria++;
                continue;
            }
            CalorSetor c = porSetor.get(s.getCodigo());
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("codigo", s.getCodigo());
            props.put("bairro", s.getNmBairro());
            props.put("pop60mais", s.getPop60mais());
            props.put("ipvsGrupo", s.getIpvsGrupo());
            props.put("classeCalor", c == null ? null : c.getClasseCalor());
            props.put("lstVeraoC", c == null ? null : c.getLstVeraoC());

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("id", s.getCodigo());
            feature.put("geometry", geometria(g));
            feature.put("properties", props);
            features.add(feature);
        }
        log.info("GeoJSON dos setores: {} features ({} setores sem geometria válida, {} com calor)",
                features.size(), semGeometria, porSetor.size());
        Map<String, Object> colecao = new LinkedHashMap<>();
        colecao.put("type", "FeatureCollection");
        colecao.put("features", features);
        return colecao;
    }

    /** Converte Polygon/MultiPolygon do JTS para a estrutura GeoJSON (coordenadas [lon, lat], 6 casas). */
    private static Map<String, Object> geometria(Geometry g) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (g.getNumGeometries() == 1 && g instanceof Polygon p) {
            m.put("type", "Polygon");
            m.put("coordinates", aneis(p));
        } else {
            List<Object> poligonos = new ArrayList<>();
            for (int i = 0; i < g.getNumGeometries(); i++) {
                if (g.getGeometryN(i) instanceof Polygon p) poligonos.add(aneis(p));
            }
            m.put("type", "MultiPolygon");
            m.put("coordinates", poligonos);
        }
        return m;
    }

    private static List<Object> aneis(Polygon p) {
        List<Object> aneis = new ArrayList<>();
        aneis.add(anel(p.getExteriorRing()));
        for (int i = 0; i < p.getNumInteriorRing(); i++) aneis.add(anel(p.getInteriorRingN(i)));
        return aneis;
    }

    private static List<double[]> anel(LinearRing r) {
        List<double[]> pontos = new ArrayList<>();
        for (Coordinate c : r.getCoordinates()) pontos.add(new double[]{arredonda(c.x), arredonda(c.y)});
        return pontos;
    }

    private static double arredonda(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}
