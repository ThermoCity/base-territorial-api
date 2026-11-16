package br.com.thermocity.baseterritorialapi.simulacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado de uma simulação (GET /simulacoes/{id}): o cenário, cada dia e cada setor por dia.
 * Os totais da busca ativa vêm do motor dia a dia (MotorSimulacao).
 */
public record SimulacaoDetalheResponse(Long id, String nome, StatusSimulacao status,
                                       OffsetDateTime criadoEm, OffsetDateTime concluidoEm, Cenario cenario,
                                       String versaoLimiar, BigDecimal maxHistoricoIcC, int nivelMaximo,
                                       int diasExtrapolados, List<Dia> dias, List<Setor> setores) {

    public record Cenario(String modeloId, LocalDate dataInicio, int qtdDias, ModoRefugios refugios,
                          int agentesPorUbs, int contatosPorAgenteDia, BigDecimal fatorK, String distrito) {
    }

    public record Dia(int dia, LocalDate data, BigDecimal icMaxC, int nivel, String nome, String regra,
                      boolean extrapolacao, Integer pessoasNaLista, Integer contatadas, Integer pendentes,
                      Integer semRefugio800m) {
    }

    /** icEstimadoC: IC do setor estimado com o fator k (estimativa; nulo sem temperatura de superfície). */
    public record Setor(int dia, String codigoSetor, int pessoasNaLista, int contatadas, int pendentes,
                        int semRefugio800m, BigDecimal icEstimadoC) {
    }

    static SimulacaoDetalheResponse de(Simulacao s, List<SimulacaoDia> dias, List<SimulacaoResultado> setores) {
        Cenario cenario = new Cenario(s.getModeloId(), s.getDataInicio(), s.getQtdDias(), s.getRefugios(),
                s.getAgentesPorUbs(), s.getContatosPorAgenteDia(), s.getFatorK(), s.getDistrito());
        return new SimulacaoDetalheResponse(s.getId(), s.getNome(), s.getStatus(), s.getCriadoEm(),
                s.getConcluidoEm(), cenario, s.getVersaoLimiar(), s.getMaxHistoricoIcC(), s.getNivelMaximo(),
                s.getDiasExtrapolados(),
                dias.stream().map(d -> new Dia(d.getId().getDia(), d.getDataLocal(), d.getIcMaxC(), d.getNivel(),
                        d.getNomeNivel(), d.getRegra(), d.getExtrapolacao() == 'S', d.getPessoasLista(),
                        d.getContatadas(), d.getPendentes(), d.getSemRefugio800m())).toList(),
                setores.stream().map(r -> new Setor(r.getId().getDia(), r.getId().getCodigoSetor(), r.getPessoasLista(),
                        r.getContatadas(), r.getPendentes(), r.getSemRefugio800m(), r.getIcEstimadoC())).toList());
    }
}
