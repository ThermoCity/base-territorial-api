package br.com.thermocity.baseterritorialapi.setor;

import br.com.thermocity.baseterritorialapi.calor.CalorSetor;

import java.time.LocalDate;

/** Dados de um setor devolvidos pela API (sem a geometria). */
public record SetorResponse(
        String codigo,
        String situacao,
        String distrito,
        String subdistrito,
        String bairro,
        Double areaKm2,
        Populacao populacao,
        Ipvs ipvs,
        Calor calor,
        Ponto pontoReferencia) {

    static final String FONTE_CALOR = "USGS/NASA - Landsat Collection 2 Level-2 (temperatura de superfície), "
            + "processado no QGIS; modelo de Amorim, Dubreuil e Cardoso (2015), Unesp";
    static final String AVISO_CALOR = "Temperatura de SUPERFÍCIE (chão e telhados) às 10h20 do dia da cena, não do ar. "
            + "Use a classe de 1 a 5 para comparar setores.";

    public record Populacao(Integer total, Integer de60a69, Integer de70mais, Integer de60mais,
                            Double percentual60mais, StatusDado status, String fonte) {
    }

    public record Ipvs(Integer grupo, String nome, String edicao, StatusDado status, String fonte) {
    }

    public record Calor(Integer classe, Double temperaturaSuperficieC, Double ndviMedio, Double percentualConstrucoes,
                        Double percentualVegetacaoDensa, Double temperaturaArEstimadaC, Integer pixelsValidos,
                        String cenaLandsat, LocalDate dataCena, StatusDado status, String fonte, String aviso) {
    }

    public record Ponto(Double lat, Double lon) {
    }

    public static SetorResponse de(Setor s, CalorSetor c) {
        return new SetorResponse(
                s.getCodigo(), s.getSituacao(), s.getNmDistrito(), s.getNmSubdistrito(), s.getNmBairro(), s.getAreaKm2(),
                new Populacao(s.getPopTotal(), s.getPop60a69(), s.getPop70mais(), s.getPop60mais(), s.getPct60mais(),
                        s.getStatusCenso(), "IBGE - Censo Demográfico 2022 (agregados por setor)"),
                new Ipvs(s.getIpvsGrupo(), s.getIpvsNome(), s.getIpvsEdicao(), s.getIpvsStatus(),
                        "Fundação Seade - Índice Paulista de Vulnerabilidade Social"),
                calor(c),
                new Ponto(s.getLatReferencia(), s.getLonReferencia()));
    }

    private static Calor calor(CalorSetor c) {
        if (c == null) {
            return new Calor(null, null, null, null, null, null, null, null, null,
                    StatusDado.SEM_REGISTRO, FONTE_CALOR, AVISO_CALOR);
        }
        return new Calor(c.getClasseCalor(), c.getLstVeraoC(), c.getNdviMedio(), c.getPctConstrucoes(),
                c.getPctVegetacaoDensa(), c.getTaEstimadaC(), c.getPixelsValidos(), c.getCenaLandsat(),
                c.getDataCena(), c.getStatus(), FONTE_CALOR, AVISO_CALOR);
    }
}
