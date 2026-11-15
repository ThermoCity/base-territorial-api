package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Resumo diário do clima (série da estação INMET A707, carregada pelo repositório do banco).
 * Só leitura: os modelos de onda de calor usam o IC máximo dos dias válidos.
 * As demais colunas da tabela não são mapeadas porque o gêmeo não precisa delas.
 */
@Entity
@Immutable
@Table(name = "TMC_RESUMO_DIARIO_CLIMA")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResumoDiarioClima {

    @EmbeddedId
    private ResumoDiarioClimaId id;

    /** Maior índice de calor horário do dia, em °C. */
    @Column(name = "ic_max_c", precision = 5, scale = 2)
    private BigDecimal icMaxC;

    /** 'S' quando o dia tem horas válidas suficientes para entrar nas estatísticas. */
    @Column(name = "dia_valido", nullable = false)
    private Character diaValido;
}
