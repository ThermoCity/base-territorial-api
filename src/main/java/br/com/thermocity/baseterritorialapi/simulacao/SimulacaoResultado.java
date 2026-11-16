package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

/**
 * Resultado de um setor num dia da simulação (gravado pelo motor dia a dia).
 * Implementa Persistable para o saveAll gravar direto (INSERT em lote) sem antes consultar cada
 * linha: a chave é montada pela aplicação e uma simulação gera até 15 × 420 linhas.
 */
@Entity
@Table(name = "TMC_SIMULACAO_RESULTADO")
@Getter
@NoArgsConstructor
public class SimulacaoResultado implements Persistable<SimulacaoResultadoId> {

    @EmbeddedId
    private SimulacaoResultadoId id;

    @Column(name = "pessoas_lista", nullable = false)
    private Integer pessoasLista;

    @Column(name = "contatadas", nullable = false)
    private Integer contatadas;

    @Column(name = "pendentes", nullable = false)
    private Integer pendentes;

    @Column(name = "sem_refugio_800m", nullable = false)
    private Integer semRefugio800m;

    /** Estimativa com o fator k; nula quando o setor não tem temperatura de superfície. */
    @Column(name = "ic_estimado_c", precision = 5, scale = 2)
    private BigDecimal icEstimadoC;

    @Transient
    private boolean novo;

    public SimulacaoResultado(SimulacaoResultadoId id, int pessoasLista, int contatadas, int pendentes,
                              int semRefugio800m, BigDecimal icEstimadoC) {
        this.id = id;
        this.pessoasLista = pessoasLista;
        this.contatadas = contatadas;
        this.pendentes = pendentes;
        this.semRefugio800m = semRefugio800m;
        this.icEstimadoC = icEstimadoC;
        this.novo = true;
    }

    @Override
    public boolean isNew() {
        return novo;
    }
}
