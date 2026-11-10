package br.com.thermocity.baseterritorialapi.setor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Setor censitário do IBGE (Censo 2022) com população por idade e IPVS. */
@Entity
@Table(name = "SETOR")
@Getter
@Setter
@NoArgsConstructor
public class Setor {

    /** Código do setor (CD_SETOR), 15 dígitos. Chave comum de todos os dados. */
    @Id
    @Column(length = 15)
    private String codigo;

    private String situacao;
    @Column(length = 5)
    private String cdSituacao;
    @Column(length = 5)
    private String cdTipo;
    private Double areaKm2;

    @Column(length = 7)
    private String cdMunicipio;
    private String nmMunicipio;
    @Column(length = 9)
    private String cdDistrito;
    private String nmDistrito;
    @Column(length = 11)
    private String cdSubdistrito;
    private String nmSubdistrito;
    private String cdBairro;
    private String nmBairro;

    // ---- Censo 2022 (PBI #3) ----
    private Integer popTotal;
    @Column(name = "pop_60_69")
    private Integer pop60a69;
    @Column(name = "pop_70_mais")
    private Integer pop70mais;
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private StatusDado statusCenso;

    // ---- IPVS 2022 (PBI #4) ----
    private Integer ipvsGrupo;
    private String ipvsNome;
    @Column(length = 10)
    private String ipvsEdicao;
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private StatusDado ipvsStatus;

    // ---- Geometria ----
    /** Ponto garantidamente dentro do setor, usado para medir distâncias. */
    private Double latReferencia;
    private Double lonReferencia;

    @Lob
    @Column(name = "geometria_wkt")
    private String geometriaWkt;

    private OffsetDateTime atualizadoEm;

    /** População com 60 anos ou mais; null se alguma faixa estiver suprimida. */
    public Integer getPop60mais() {
        return (pop60a69 == null || pop70mais == null) ? null : pop60a69 + pop70mais;
    }

    /** Percentual de 60+ no setor (uma casa decimal); null se não houver dado. */
    public Double getPct60mais() {
        Integer p60 = getPop60mais();
        if (p60 == null || popTotal == null || popTotal == 0) return null;
        return Math.round(1000.0 * p60 / popTotal) / 10.0;
    }
}
