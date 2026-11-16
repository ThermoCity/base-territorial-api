package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.common.PaginaResponse;
import br.com.thermocity.baseterritorialapi.common.RecursoNaoEncontradoException;
import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Cria e consulta simulações do gêmeo digital (PBI "Criar e consultar cenários de simulação").
 * O cenário e o resultado ficam só nas tabelas TMC_SIMULACAO*: nada das tabelas reais é alterado.
 */
@Service
public class SimulacaoService {

    static final int CONTATOS_POR_AGENTE_PADRAO = 15;
    static final BigDecimal FATOR_K_PADRAO = new BigDecimal("0.25");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final SimulacaoRepository simulacoes;
    private final SimulacaoDiaRepository dias;
    private final SimulacaoResultadoRepository resultados;
    private final NivelCalorCalculadora calculadora;
    private final ModeloOndaCalorService modelos;
    private final SetorRepository setores;

    public SimulacaoService(SimulacaoRepository simulacoes, SimulacaoDiaRepository dias,
                            SimulacaoResultadoRepository resultados, NivelCalorCalculadora calculadora,
                            ModeloOndaCalorService modelos, SetorRepository setores) {
        this.simulacoes = simulacoes;
        this.dias = dias;
        this.resultados = resultados;
        this.calculadora = calculadora;
        this.modelos = modelos;
        this.setores = setores;
    }

    @Transactional
    public Simulacao criar(CriarSimulacaoRequest req) {
        String distrito = req.distrito() == null || req.distrito().isBlank() ? null : req.distrito().trim();
        if (distrito != null && !setores.existsByNmDistritoIgnoreCase(distrito)) {
            throw new RequisicaoInvalidaException("distrito: não há setores no distrito '" + distrito + "'");
        }

        List<LimiarNivelCalor> regras = calculadora.regrasAtivas();
        List<NivelDoDia> niveis = NivelCalorCalculadora.classificar(regras, req.icMaxPorDia(), null);
        BigDecimal maxHistorico = modelos.maxHistorico();

        Simulacao s = new Simulacao();
        s.setNome(req.nome() == null || req.nome().isBlank()
                ? "Simulação de " + niveis.size() + (niveis.size() == 1 ? " dia" : " dias")
                : req.nome().trim());
        s.setStatus(StatusSimulacao.PROCESSANDO);
        s.setModeloId(req.modeloId());
        s.setDataInicio(req.dataInicio());
        s.setQtdDias((short) niveis.size());
        s.setRefugios(req.refugios() == null ? ModoRefugios.TODOS : req.refugios());
        s.setAgentesPorUbs(req.agentesPorUbs().shortValue());
        s.setContatosPorAgenteDia((short) (req.contatosPorAgenteDia() == null
                ? CONTATOS_POR_AGENTE_PADRAO : req.contatosPorAgenteDia()));
        s.setFatorK((req.fatorK() == null ? FATOR_K_PADRAO : req.fatorK()).setScale(2, RoundingMode.HALF_UP));
        s.setDistrito(distrito);
        s.setVersaoLimiar(regras.get(0).getVersaoLimiar());
        s.setMaxHistoricoIcC(maxHistorico);
        s.setCriadoEm(OffsetDateTime.now(FUSO));

        List<SimulacaoDia> linhas = new ArrayList<>();
        short nivelMaximo = 1;
        short extrapolados = 0;
        for (NivelDoDia n : niveis) {
            BigDecimal ic = BigDecimal.valueOf(n.icMaxC()).setScale(2, RoundingMode.HALF_UP);
            boolean extrapolacao = maxHistorico != null && ic.compareTo(maxHistorico) > 0;
            if (extrapolacao) extrapolados++;
            nivelMaximo = (short) Math.max(nivelMaximo, n.nivel());
            SimulacaoDia d = new SimulacaoDia();
            d.setDataLocal(req.dataInicio() == null ? null : req.dataInicio().plusDays(n.dia() - 1L));
            d.setIcMaxC(ic);
            d.setNivel((short) n.nivel());
            d.setNomeNivel(n.nome());
            d.setRegra(n.regra());
            d.setExtrapolacao(extrapolacao ? 'S' : 'N');
            linhas.add(d);
        }
        s.setNivelMaximo(nivelMaximo);
        s.setDiasExtrapolados(extrapolados);
        simulacoes.save(s);

        for (int i = 0; i < linhas.size(); i++) {
            linhas.get(i).setId(new SimulacaoDiaId(s.getId(), (short) (i + 1)));
        }
        dias.saveAll(linhas);

        s.setStatus(StatusSimulacao.CONCLUIDA);
        s.setConcluidoEm(OffsetDateTime.now(FUSO));
        return s;
    }

    @Transactional(readOnly = true)
    public SimulacaoDetalheResponse detalhar(Long id, Integer dia) {
        Simulacao s = simulacoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulação não encontrada: " + id));
        if (dia != null && (dia < 1 || dia > s.getQtdDias())) {
            throw new RequisicaoInvalidaException("dia deve estar entre 1 e " + s.getQtdDias());
        }
        List<SimulacaoResultado> porSetor = dia == null
                ? resultados.findByIdIdSimulacaoOrderByIdDiaAscIdCodigoSetorAsc(id)
                : resultados.findByIdIdSimulacaoAndIdDiaOrderByIdCodigoSetor(id, dia.shortValue());
        return SimulacaoDetalheResponse.de(s, dias.findByIdIdSimulacaoOrderByIdDia(id), porSetor);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<SimulacaoResumoResponse> listar(Pageable pageable) {
        return PaginaResponse.de(simulacoes.findAll(pageable), SimulacaoResumoResponse::de);
    }
}
