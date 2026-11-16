package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.common.PaginaResponse;
import br.com.thermocity.baseterritorialapi.common.RecursoNaoEncontradoException;
import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import br.com.thermocity.baseterritorialapi.calor.CalorSetor;
import br.com.thermocity.baseterritorialapi.calor.CalorSetorRepository;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.servico.Servico;
import br.com.thermocity.baseterritorialapi.servico.ServicoService;
import br.com.thermocity.baseterritorialapi.setor.Setor;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import br.com.thermocity.baseterritorialapi.sintetico.PessoaSintetica;
import br.com.thermocity.baseterritorialapi.sintetico.PessoaSinteticaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cria, roda e consulta simulações do gêmeo digital (PBIs 121 e 107).
 * Carrega uma cópia da cidade (setores, nota de calor, pessoas, refúgios), roda o MotorSimulacao e
 * grava o resultado só nas tabelas TMC_SIMULACAO*: nada das tabelas reais é alterado.
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
    private final CalorSetorRepository calor;
    private final PessoaSinteticaRepository pessoas;
    private final ServicoService servicos;

    public SimulacaoService(SimulacaoRepository simulacoes, SimulacaoDiaRepository dias,
                            SimulacaoResultadoRepository resultados, NivelCalorCalculadora calculadora,
                            ModeloOndaCalorService modelos, SetorRepository setores, CalorSetorRepository calor,
                            PessoaSinteticaRepository pessoas, ServicoService servicos) {
        this.simulacoes = simulacoes;
        this.dias = dias;
        this.resultados = resultados;
        this.calculadora = calculadora;
        this.modelos = modelos;
        this.setores = setores;
        this.calor = calor;
        this.pessoas = pessoas;
        this.servicos = servicos;
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

        // motor dia a dia sobre uma cópia da cidade
        Map<String, MotorSimulacao.Setor> cidade = copiaDosSetores(distrito);
        List<MotorSimulacao.Pessoa> populacao = copiaDasPessoas(cidade, s.getRefugios());
        int capacidade = s.getAgentesPorUbs() * s.getContatosPorAgenteDia();
        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis, populacao, cidade, capacidade, s.getFatorK());

        for (int i = 0; i < linhas.size(); i++) {
            SimulacaoDia d = linhas.get(i);
            MotorSimulacao.Dia total = r.dias().get(i);
            d.setId(new SimulacaoDiaId(s.getId(), (short) (i + 1)));
            d.setPessoasLista(total.pessoasNaLista());
            d.setContatadas(total.contatadas());
            d.setPendentes(total.pendentes());
            d.setSemRefugio800m(total.semRefugio800m());
        }
        dias.saveAll(linhas);
        dias.flush(); // os dias precisam existir antes dos setores (chave estrangeira)
        resultados.saveAll(r.setores().stream()
                .map(x -> new SimulacaoResultado(new SimulacaoResultadoId(s.getId(), (short) x.dia(), x.setor()),
                        x.pessoasNaLista(), x.contatadas(), x.pendentes(), x.semRefugio800m(), x.icEstimadoC()))
                .toList());

        s.setStatus(StatusSimulacao.CONCLUIDA);
        s.setConcluidoEm(OffsetDateTime.now(FUSO));
        return s;
    }

    /** Setores da simulação (todos ou só os do distrito) com IPVS, nota de calor e temperatura de superfície. */
    private Map<String, MotorSimulacao.Setor> copiaDosSetores(String distrito) {
        Map<String, CalorSetor> calorPorSetor = calor.findAll().stream()
                .collect(Collectors.toMap(CalorSetor::getCodigoSetor, Function.identity()));
        Map<String, MotorSimulacao.Setor> cidade = new HashMap<>();
        for (Setor setor : setores.findAll()) {
            if (distrito != null && !distrito.equalsIgnoreCase(setor.getNmDistrito())) continue;
            CalorSetor c = calorPorSetor.get(setor.getCodigo());
            cidade.put(setor.getCodigo(), new MotorSimulacao.Setor(setor.getCodigo(), setor.getIpvsGrupo(),
                    c == null ? null : c.getClasseCalor(), c == null ? null : c.getLstVeraoC()));
        }
        return cidade;
    }

    /**
     * Pessoas 60+ (cadastro sintético, com consentimento) dos setores da simulação, com a distância
     * até o refúgio considerado mais próximo. Sem coordenada própria, usa o ponto de referência do setor.
     */
    private List<MotorSimulacao.Pessoa> copiaDasPessoas(Map<String, MotorSimulacao.Setor> cidade, ModoRefugios modo) {
        List<Servico> refugios = switch (modo) {
            case TODOS -> servicos.refugiosComCoordenada(true);
            case PUBLICOS -> servicos.refugiosComCoordenada(false);
            case NENHUM -> List.of();
        };
        Map<String, Setor> referencia = setores.findAllById(cidade.keySet()).stream()
                .collect(Collectors.toMap(Setor::getCodigo, Function.identity()));
        List<MotorSimulacao.Pessoa> lista = new ArrayList<>();
        for (PessoaSintetica p : pessoas.findAll()) {
            if (!cidade.containsKey(p.getSetorCodigo()) || !p.isConsentimentoLgpd()
                    || p.getIdade() == null || p.getIdade() < 60) continue;
            Double lat = p.getLat();
            Double lon = p.getLon();
            if (lat == null || lon == null) {
                Setor s = referencia.get(p.getSetorCodigo());
                lat = s == null ? null : s.getLatReferencia();
                lon = s == null ? null : s.getLonReferencia();
            }
            lista.add(new MotorSimulacao.Pessoa(p.getId(), p.getIdade(), p.getSetorCodigo(),
                    p.getUbsReferenciaId() == null ? "SEM_UBS" : p.getUbsReferenciaId(),
                    p.isMoraSozinho(), p.isMobilidadeReduzida(), p.isCondicaoCronica(),
                    distanciaAoRefugio(lat, lon, refugios)));
        }
        return lista;
    }

    private static Double distanciaAoRefugio(Double lat, Double lon, List<Servico> refugios) {
        if (lat == null || lon == null || refugios.isEmpty()) return null;
        double menor = Double.MAX_VALUE;
        for (Servico r : refugios) {
            menor = Math.min(menor, GeoService.distanciaMetros(lat, lon, r.getLat(), r.getLon()));
        }
        return menor;
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
