package br.com.thermocity.baseterritorialapi.servico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** UBS (CNES) ou possível refúgio climático (OpenStreetMap). PBI #5. */
@Entity
@Table(name = "SERVICO")
@Getter
@Setter
@NoArgsConstructor
public class Servico {

    /** Ex.: CNES-2062801 ou OSM-way-229425712. */
    @Id
    @Column(length = 40)
    private String id;

    @Column(length = 300)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoServico tipo;

    private String categoria;
    @Column(length = 300)
    private String endereco;
    private String bairro;
    private Double lat;
    private Double lon;
    @Column(length = 30)
    private String fonte;
    @Column(length = 40)
    private String idFonte;

    /** Setor onde o ponto está (calculado na importação). */
    @Column(length = 15)
    private String setorCodigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusCoordenada coordStatus;

    /** Só para refúgios: manter, potencial, revisar, remover ou publico_alvo. */
    @Column(length = 20)
    private String revisao;

    @Column(length = 500)
    private String motivoRevisao;

    public boolean temCoordenada() {
        return coordStatus == StatusCoordenada.OK && lat != null && lon != null;
    }
}
