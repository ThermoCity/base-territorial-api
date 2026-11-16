package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Resultado de um setor num dia da simulação (gravado pelo motor dia a dia). */
@Entity
@Table(name = "TMC_SIMULACAO_RESULTADO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoResultado {

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
}
