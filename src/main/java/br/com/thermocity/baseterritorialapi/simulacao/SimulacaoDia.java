package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um dia do cenário: IC informado, nível NC calculado pelos limiares do banco e, quando o motor
 * dia a dia rodar, os totais da busca ativa (nulos até lá).
 */
@Entity
@Table(name = "TMC_SIMULACAO_DIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoDia {

    @EmbeddedId
    private SimulacaoDiaId id;

    @Column(name = "data_local")
    private LocalDate dataLocal;

    @Column(name = "ic_max_c", precision = 5, scale = 2, nullable = false)
    private BigDecimal icMaxC;

    @Column(name = "nivel", nullable = false)
    private Short nivel;

    @Column(name = "nome_nivel", length = 20, nullable = false)
    private String nomeNivel;

    @Column(name = "regra", length = 300, nullable = false)
    private String regra;

    /** 'S' quando o IC passa do maior valor já registrado na estação A707. */
    @Column(name = "extrapolacao", nullable = false)
    private Character extrapolacao;

    @Column(name = "pessoas_lista")
    private Integer pessoasLista;

    @Column(name = "contatadas")
    private Integer contatadas;

    @Column(name = "pendentes")
    private Integer pendentes;

    @Column(name = "sem_refugio_800m")
    private Integer semRefugio800m;
}
