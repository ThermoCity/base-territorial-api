package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/** Chave de TMC_RESUMO_DIARIO_CLIMA: um resumo por dia, fonte e tipo (observação ou previsão). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResumoDiarioClimaId implements Serializable {

    @Column(name = "data_local", nullable = false)
    private LocalDate dataLocal;

    @Column(name = "fonte", length = 20, nullable = false)
    private String fonte;

    @Column(name = "tipo", length = 10, nullable = false)
    private String tipo;
}
