package br.com.thermocity.baseterritorialapi.simulacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Uma simulação na lista (GET /simulacoes) e na resposta do POST /simulacoes. */
public record SimulacaoResumoResponse(Long id, String nome, StatusSimulacao status, String modeloId,
                                      LocalDate dataInicio, int qtdDias, int nivelMaximo, int diasExtrapolados,
                                      ModoRefugios refugios, int agentesPorUbs, String distrito,
                                      OffsetDateTime criadoEm) {

    static SimulacaoResumoResponse de(Simulacao s) {
        return new SimulacaoResumoResponse(s.getId(), s.getNome(), s.getStatus(), s.getModeloId(), s.getDataInicio(),
                s.getQtdDias(), s.getNivelMaximo(), s.getDiasExtrapolados(), s.getRefugios(), s.getAgentesPorUbs(),
                s.getDistrito(), s.getCriadoEm());
    }
}
