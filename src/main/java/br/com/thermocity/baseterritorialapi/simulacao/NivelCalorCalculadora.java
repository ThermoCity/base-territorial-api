package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula o nível de calor (NC1–NC5) de cada dia de um cenário a partir dos limiares do banco,
 * com as mesmas regras do modo real:
 * <ul>
 *   <li>cada regra é "IC máx ≥ limiar por N dias seguidos" (N = 1 ou 3);</li>
 *   <li>o nível do dia é o maior nível cuja regra foi cumprida; se nenhuma for, NC1;</li>
 *   <li>a contagem de dias seguidos olha para trás, inclusive os dias anteriores ao cenário, se informados.</li>
 * </ul>
 * É a mesma lógica do script que gerou os limiares (analise_niveis_calor.py, nivel_para_sequencia).
 */
@Service
public class NivelCalorCalculadora {

    private static final char ATIVO = 'S';

    private final LimiarNivelCalorRepository limiares;

    public NivelCalorCalculadora(LimiarNivelCalorRepository limiares) {
        this.limiares = limiares;
    }

    /** Resultado com a versão dos limiares usada, para o resultado da simulação ser rastreável. */
    public record Resultado(String versaoLimiar, List<NivelDoDia> dias) {
    }

    @Transactional(readOnly = true)
    public Resultado classificar(List<Double> icMaxPorDia, List<Double> icMaxDiasAnteriores) {
        List<LimiarNivelCalor> regras = limiares.findByAtivoOrderByNivelAscDuracaoDiasAsc(ATIVO);
        if (regras.isEmpty()) {
            throw new RequisicaoInvalidaException(
                    "Os limiares de nível de calor não estão carregados (TMC_LIMIAR_NIVEL_CALOR)");
        }
        return new Resultado(regras.get(0).getVersaoLimiar(), classificar(regras, icMaxPorDia, icMaxDiasAnteriores));
    }

    /** Cálculo puro, sem banco: usado pelo serviço e pelos testes. */
    static List<NivelDoDia> classificar(List<LimiarNivelCalor> regras, List<Double> icMaxPorDia,
                                        List<Double> icMaxDiasAnteriores) {
        List<LimiarNivelCalor> ordenadas = regras.stream()
                .sorted(Comparator.comparing(LimiarNivelCalor::getNivel).thenComparing(LimiarNivelCalor::getDuracaoDias))
                .toList();
        LimiarNivelCalor base = ordenadas.stream()
                .filter(r -> r.getNivel() == 1)
                .findFirst()
                .orElseThrow(() -> new RequisicaoInvalidaException("A tabela de limiares não tem a linha do NC1"));

        // histórico + cenário numa sequência só: a regra de 3 dias pode começar antes do cenário
        List<Double> sequencia = new ArrayList<>(icMaxDiasAnteriores == null ? List.of() : icMaxDiasAnteriores);
        int inicioCenario = sequencia.size();
        sequencia.addAll(icMaxPorDia);

        List<NivelDoDia> dias = new ArrayList<>(icMaxPorDia.size());
        for (int i = inicioCenario; i < sequencia.size(); i++) {
            LimiarNivelCalor vencedora = base;
            for (LimiarNivelCalor r : ordenadas) {
                // ">" mantém, no mesmo nível, a regra de 1 dia (vem antes na ordenação)
                if (r.getIcMinC() != null && r.getNivel() > vencedora.getNivel() && cumpre(r, sequencia, i)) {
                    vencedora = r;
                }
            }
            dias.add(new NivelDoDia(i - inicioCenario + 1, sequencia.get(i), vencedora.getNivel(),
                    vencedora.getNomeNivel(), vencedora.getRegra(), vencedora.getAcaoEsperada()));
        }
        return dias;
    }

    /** O IC ficou ≥ limiar nos últimos duracaoDias dias, terminando no dia i? */
    private static boolean cumpre(LimiarNivelCalor regra, List<Double> sequencia, int i) {
        int duracao = regra.getDuracaoDias();
        if (i - duracao + 1 < 0) return false; // ainda não há dias suficientes
        double limiar = regra.getIcMinC().doubleValue();
        for (int k = i - duracao + 1; k <= i; k++) {
            Double ic = sequencia.get(k);
            if (ic == null || ic < limiar) return false;
        }
        return true;
    }
}
