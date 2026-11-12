package br.com.thermocity.baseterritorialapi.sintetico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Pessoa 60+ FICTÍCIA (PBI #6). Nenhum dado aqui corresponde a uma pessoa real.
 * Serve para testar a busca ativa sem tocar em dados pessoais (LGPD).
 */
@Entity
@Table(name = "TMC_PESSOA_SINTETICA", indexes = @Index(name = "IX_TMC_PESSOA_SETOR", columnList = "setor_codigo"))
@Getter
@Setter
@NoArgsConstructor
public class PessoaSintetica {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pessoa_sintetica")
    @SequenceGenerator(name = "seq_pessoa_sintetica", sequenceName = "TMC_SEQ_PESSOA_SINTETICA", allocationSize = 50)
    private Long id;

    @Column(length = 120, nullable = false)
    private String nome;

    private Integer idade;

    @Column(name = "faixa_etaria", length = 10)
    private String faixaEtaria;

    @Column(length = 1)
    private String sexo;

    @Column(name = "setor_codigo", length = 15, nullable = false)
    private String setorCodigo;

    private Double lat;
    private Double lon;

    @Column(name = "mora_sozinho")
    private boolean moraSozinho;

    @Column(name = "mobilidade_reduzida")
    private boolean mobilidadeReduzida;

    @Column(name = "condicao_cronica")
    private boolean condicaoCronica;

    @Column(length = 30)
    private String telefone;

    @Column(name = "contato_ref_nome", length = 120)
    private String contatoReferenciaNome;

    @Column(name = "contato_ref_telefone", length = 30)
    private String contatoReferenciaTelefone;

    @Column(name = "ubs_ref_id", length = 40)
    private String ubsReferenciaId;

    @Column(name = "ubs_ref_nome", length = 300)
    private String ubsReferenciaNome;

    @Column(name = "consentimento_lgpd")
    private boolean consentimentoLgpd;

    @Column(name = "consentimento_em")
    private OffsetDateTime consentimentoEm;

    private boolean sintetico = true;

    private Long semente;

    @Column(name = "gerado_em")
    private OffsetDateTime geradoEm;
}
