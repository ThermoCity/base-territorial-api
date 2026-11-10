package br.com.thermocity.baseterritorialapi.calor;

import br.com.thermocity.baseterritorialapi.setor.StatusDado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Indicadores de calor de um setor censitário, calculados no QGIS a partir de uma cena Landsat de verão
 * (feature "Mapa de Calor por Setor"). Uma linha por setor, ligada ao SETOR pelo código.
 */
@Entity
@Table(name = "CALOR_SETOR")
@Getter
@Setter
@NoArgsConstructor
public class CalorSetor {

    @Id
    @Column(name = "codigo_setor", length = 15)
    private String codigoSetor;

    /** Temperatura média da SUPERFÍCIE (chão, telhados) no setor, em °C. Não é temperatura do ar. */
    @Column(name = "lst_verao_c")
    private Double lstVeraoC;

    @Column(name = "ndvi_medio")
    private Double ndviMedio;

    /** % de construções numa janela de 990 m em volta de cada pixel (variável C1 do modelo da Unesp). */
    @Column(name = "pct_construcoes")
    private Double pctConstrucoes;

    /** % de vegetação densa numa janela de 990 m em volta de cada pixel (variável C3 do modelo da Unesp). */
    @Column(name = "pct_vegetacao_densa")
    private Double pctVegetacaoDensa;

    /** Temperatura do ar estimada pelo modelo da Unesp. Serve só para comparar setores (viés de ~+9 °C). */
    @Column(name = "ta_estimada_c")
    private Double taEstimadaC;

    /** 1 = 20% mais frescos ... 5 = 20% mais quentes (quintis da temperatura de superfície). */
    @Column(name = "classe_calor")
    private Integer classeCalor;

    @Column(name = "pixels_validos")
    private Integer pixelsValidos;

    @Column(name = "cena_landsat", length = 60)
    private String cenaLandsat;

    @Column(name = "data_cena")
    private LocalDate dataCena;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private StatusDado status;

    private OffsetDateTime atualizadoEm;
}
