package br.com.thermocity.baseterritorialapi.simulacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Cenário de simulação enviado pelo gestor. Fica separado das tabelas reais. */
@Entity
@Table(name = "TMC_SIMULACAO")
@Getter
@Setter
@NoArgsConstructor
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_simulacao")
    private Long id;

    @Column(name = "nome", length = 120, nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private StatusSimulacao status;

    /** Modelo pronto usado como ponto de partida (GET /simulacoes/modelos), se houver. */
    @Column(name = "modelo_id", length = 40)
    private String modeloId;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "qtd_dias", nullable = false)
    private Short qtdDias;

    @Enumerated(EnumType.STRING)
    @Column(name = "refugios", length = 10, nullable = false)
    private ModoRefugios refugios;

    @Column(name = "agentes_por_ubs", nullable = false)
    private Short agentesPorUbs;

    @Column(name = "contatos_por_agente_dia", nullable = false)
    private Short contatosPorAgenteDia;

    /** Estimativa (0,10 a 0,50) de quanto da diferença de temperatura de superfície vira diferença de ar. */
    @Column(name = "fator_k", precision = 3, scale = 2, nullable = false)
    private BigDecimal fatorK;

    /** Recorte da simulação; nulo = cidade toda. */
    @Column(name = "distrito")
    private String distrito;

    @Column(name = "versao_limiar", length = 40, nullable = false)
    private String versaoLimiar;

    @Column(name = "nivel_maximo", nullable = false)
    private Short nivelMaximo;

    @Column(name = "max_historico_ic_c", precision = 5, scale = 2)
    private BigDecimal maxHistoricoIcC;

    @Column(name = "dias_extrapolados", nullable = false)
    private Short diasExtrapolados;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;
}
