package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelos prontos de onda de calor (PBI "Disponibilizar modelos prontos de onda de calor").
 * <p>
 * Um episódio é uma sequência de dias seguidos em NC4 ou NC5 na série histórica, com o nível calculado
 * pelos limiares ativos do banco (as mesmas regras de POST /simulacoes/niveis). Cada modelo leva o episódio
 * e até 2 dias antes e 2 depois, para o gestor ver a entrada e a saída da onda, limitado a 15 dias.
 * As variações somam um valor fixo ao IC de todos os dias e recalculam o nível; o dia que passa do
 * maior IC já registrado na estação é marcado como extrapolação.
 */
@Service
public class ModeloOndaCalorService {

    static final int DIAS_ANTES = 2;
    static final int DIAS_DEPOIS = 2;
    static final int MAX_DIAS_MODELO = 15;
    static final int NIVEL_EPISODIO = 4;
    static final double MAX_ACRESCIMO_C = 5.0;
    static final int MAX_VARIACOES = 4;

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_MES_ANO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ResumoDiarioClimaRepository resumos;
    private final NivelCalorCalculadora calculadora;
    private final String fonte;
    private final String tipo;

    public ModeloOndaCalorService(ResumoDiarioClimaRepository resumos, NivelCalorCalculadora calculadora,
                                  @Value("${thermocity.modelos.fonte:INMET}") String fonte,
                                  @Value("${thermocity.modelos.tipo:OBSERVACAO}") String tipo) {
        this.resumos = resumos;
        this.calculadora = calculadora;
        this.fonte = fonte;
        this.tipo = tipo;
    }

    @Transactional(readOnly = true)
    public ModelosOndaCalorResponse listar(List<Double> acrescimos, int duracaoMinima) {
        validar(acrescimos, duracaoMinima);
        List<LimiarNivelCalor> regras = calculadora.regrasAtivas();
        List<DiaClima> serie = resumos.serieDiaria(fonte, tipo, 'S');
        return montar(regras, serie, acrescimos, duracaoMinima, "INMET A707 · TMC_RESUMO_DIARIO_CLIMA");
    }

    private static void validar(List<Double> acrescimos, int duracaoMinima) {
        if (duracaoMinima < 1 || duracaoMinima > 10) {
            throw new RequisicaoInvalidaException("duracaoMinima deve estar entre 1 e 10 dias");
        }
        if (acrescimos.size() > MAX_VARIACOES) {
            throw new RequisicaoInvalidaException("Informe no máximo " + MAX_VARIACOES + " acréscimos");
        }
        for (Double a : acrescimos) {
            if (a == null || a <= 0 || a > MAX_ACRESCIMO_C) {
                throw new RequisicaoInvalidaException("Cada acréscimo deve ser maior que 0 e até " + MAX_ACRESCIMO_C + " °C");
            }
        }
    }

    /** Cálculo puro, sem banco: usado pelo serviço e pelos testes. */
    static ModelosOndaCalorResponse montar(List<LimiarNivelCalor> regras, List<DiaClima> serie,
                                           List<Double> acrescimos, int duracaoMinima, String fonte) {
        String versao = regras.get(0).getVersaoLimiar();
        if (serie.isEmpty()) {
            return new ModelosOndaCalorResponse(fonte, versao, null, List.of());
        }
        double maxHistorico = serie.stream().mapToDouble(DiaClima::icMaxC).max().orElseThrow();

        List<Double> variacoes = new ArrayList<>();
        variacoes.add(0.0);
        acrescimos.stream().distinct().sorted().forEach(variacoes::add);

        List<ModelosOndaCalorResponse.Modelo> modelos = new ArrayList<>();
        for (List<DiaClima> trecho : trechosContinuos(serie)) {
            List<Double> ic = trecho.stream().map(DiaClima::icMaxC).toList();
            List<NivelDoDia> niveis = NivelCalorCalculadora.classificar(regras, ic, null);

            int i = 0;
            while (i < niveis.size()) {
                if (niveis.get(i).nivel() < NIVEL_EPISODIO) {
                    i++;
                    continue;
                }
                int inicio = i;
                while (i < niveis.size() && niveis.get(i).nivel() >= NIVEL_EPISODIO) i++;
                int fim = i - 1;
                if (fim - inicio + 1 >= duracaoMinima) {
                    for (double acrescimo : variacoes) {
                        modelos.add(modelo(regras, trecho, inicio, fim, acrescimo, maxHistorico));
                    }
                }
            }
        }
        // mais recentes primeiro; dentro do episódio, real e depois as variações em ordem crescente
        List<ModelosOndaCalorResponse.Modelo> ordenados = new ArrayList<>();
        for (int k = modelos.size(); k > 0; k -= variacoes.size()) {
            ordenados.addAll(modelos.subList(k - variacoes.size(), k));
        }
        return new ModelosOndaCalorResponse(fonte, versao, maxHistorico, ordenados);
    }

    private static ModelosOndaCalorResponse.Modelo modelo(List<LimiarNivelCalor> regras, List<DiaClima> trecho,
                                                         int inicio, int fim, double acrescimo, double maxHistorico) {
        int de = Math.max(0, inicio - DIAS_ANTES);
        int ate = Math.min(Math.min(trecho.size() - 1, fim + DIAS_DEPOIS), de + MAX_DIAS_MODELO - 1);

        // dias logo antes da janela entram só no cálculo (regra dos 3 dias seguidos), não no modelo
        List<Double> anteriores = new ArrayList<>();
        for (int k = Math.max(0, de - 2); k < de; k++) anteriores.add(somar(trecho.get(k).icMaxC(), acrescimo));
        List<Double> janela = new ArrayList<>();
        for (int k = de; k <= ate; k++) janela.add(somar(trecho.get(k).icMaxC(), acrescimo));

        List<NivelDoDia> niveis = NivelCalorCalculadora.classificar(regras, janela, anteriores);
        List<ModelosOndaCalorResponse.Dia> dias = new ArrayList<>();
        int nivelMaximo = 1;
        double icMax = Double.NEGATIVE_INFINITY;
        int extrapolados = 0;
        for (int k = 0; k < niveis.size(); k++) {
            NivelDoDia n = niveis.get(k);
            int indice = de + k;
            boolean extrapolacao = n.icMaxC() > maxHistorico;
            if (extrapolacao) extrapolados++;
            nivelMaximo = Math.max(nivelMaximo, n.nivel());
            icMax = Math.max(icMax, n.icMaxC());
            dias.add(new ModelosOndaCalorResponse.Dia(n.dia(), trecho.get(indice).data(), n.icMaxC(), n.nivel(), n.nome(),
                    indice >= inicio && indice <= fim, extrapolacao));
        }

        LocalDate dataInicio = trecho.get(inicio).data();
        LocalDate dataFim = trecho.get(fim).data();
        String sufixoId = acrescimo == 0 ? "" : "+" + numero(acrescimo);
        String nome = "Onda de calor de " + periodo(dataInicio, dataFim)
                + (acrescimo == 0 ? "" : " + " + numero(acrescimo) + " °C");
        return new ModelosOndaCalorResponse.Modelo("A707-" + dataInicio + sufixoId, nome,
                acrescimo == 0 ? "REAL" : "VARIACAO", acrescimo, dataInicio, dataFim, fim - inicio + 1,
                nivelMaximo, icMax, extrapolados, dias);
    }

    /** Separa a série em trechos sem buraco de data: dia faltando interrompe a contagem de dias seguidos. */
    static List<List<DiaClima>> trechosContinuos(List<DiaClima> serie) {
        List<List<DiaClima>> trechos = new ArrayList<>();
        List<DiaClima> atual = new ArrayList<>();
        for (DiaClima d : serie) {
            if (!atual.isEmpty() && !atual.get(atual.size() - 1).data().plusDays(1).equals(d.data())) {
                trechos.add(atual);
                atual = new ArrayList<>();
            }
            atual.add(d);
        }
        if (!atual.isEmpty()) trechos.add(atual);
        return trechos;
    }

    private static double somar(double ic, double acrescimo) {
        return BigDecimal.valueOf(ic).add(BigDecimal.valueOf(acrescimo)).doubleValue();
    }

    private static String numero(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String periodo(LocalDate inicio, LocalDate fim) {
        if (inicio.equals(fim)) return inicio.format(DIA_MES_ANO);
        return (inicio.getYear() == fim.getYear() ? inicio.format(DIA_MES) : inicio.format(DIA_MES_ANO))
                + " a " + fim.format(DIA_MES_ANO);
    }
}
