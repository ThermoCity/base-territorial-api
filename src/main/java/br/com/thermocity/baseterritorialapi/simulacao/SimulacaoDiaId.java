package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SimulacaoDiaId implements Serializable {

    @Column(name = "id_simulacao", nullable = false)
    private Long idSimulacao;

    @Column(name = "dia", nullable = false)
    private Short dia;
}
