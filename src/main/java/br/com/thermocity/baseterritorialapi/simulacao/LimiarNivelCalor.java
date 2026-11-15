package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Uma regra de entrada em um nível de calor (NC1–NC5), carregada pelo repositório do banco
 * (série INMET A707, versão A707-2003-2026-v1). Regra: IC máx diário ≥ icMinC por duracaoDias dias seguidos.
 * NC1 não tem IC mínimo: é o nível de quando nenhuma outra regra é cumprida.
 */
@Entity
@Table(name = "TMC_LIMIAR_NIVEL_CALOR")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LimiarNivelCalor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_limiar")
    private Long id;

    @Column(name = "versao_limiar", length = 40, nullable = false)
    private String versaoLimiar;

    @Column(name = "nivel", nullable = false)
    private Short nivel;

    @Column(name = "nome_nivel", length = 20, nullable = false)
    private String nomeNivel;

    /** IC máximo diário mínimo para a regra valer, em °C. Nulo no NC1. */
    @Column(name = "ic_min_c", precision = 5, scale = 2)
    private BigDecimal icMinC;

    /** Quantos dias seguidos o IC precisa ficar acima do mínimo (1 = um dia basta). */
    @Column(name = "duracao_dias", nullable = false)
    private Short duracaoDias;

    @Column(name = "regra", length = 300, nullable = false)
    private String regra;

    @Column(name = "acao_esperada", length = 400, nullable = false)
    private String acaoEsperada;

    /** 'S' = versão em uso. */
    @Column(name = "ativo", nullable = false)
    private Character ativo;
}
