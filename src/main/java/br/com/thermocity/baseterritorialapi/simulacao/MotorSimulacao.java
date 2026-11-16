package br.com.thermocity.baseterritorialapi.simulacao;

import br.com.thermocity.baseterritorialapi.buscaativa.AlcanceBuscaAtiva;
import br.com.thermocity.baseterritorialapi.buscaativa.RegraPrioridade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Motor dia a dia da simulação (PBI 107). Cálculo puro, sem banco: recebe uma cópia da cidade
 * (pessoas, setores) e o nível de cada dia, e devolve os números de cada dia e de cada setor.
 * <p>
 * Em cada dia:
 * <ol>
 *   <li>a lista do dia são as pessoas no alcance do nível (AlcanceBuscaAtiva), ordenadas pela
 *       prioridade (RegraPrioridade);</li>
 *   <li>quem ficou pendente no dia anterior entra na frente da fila, mesmo que o nível tenha caído;
 *       cada pessoa aparece uma vez só;</li>
 *   <li>cada UBS contata no máximo agentes × contatos por agente por dia, seguindo a fila;
 *       o resto fica pendente para o dia seguinte;</li>
 *   <li>conta quem da fila está a mais de 800 m do refúgio considerado mais próximo
 *       (sem refúgio considerado = todos).</li>
 * </ol>
 * Também estima o índice de calor de cada setor com o fator k:
 * IC do setor ≈ IC da cidade + k × (temperatura de superfície do setor − média dos setores).
 * É uma estimativa: a superfície (Landsat) não é a temperatura do ar.
 */
public final class MotorSimulacao {

    private MotorSimulacao() {
    }

    /** Uma pessoa da cópia da cidade. distanciaRefugioM nulo = nenhum refúgio considerado. */
    public record Pessoa(long id, int idade, String setor, String ubs, boolean moraSozinho,
                         boolean mobilidadeReduzida, boolean condicaoCronica, Double distanciaRefugioM) {

        boolean semRefugioProximo() {
            return RegraPrioridade.semRefugioProximo(distanciaRefugioM);
        }
    }

    public record Setor(String codigo, Integer ipvsGrupo, Integer notaCalor, Double lstC) {
    }

    public record Dia(int dia, int pessoasNaLista, int contatadas, int pendentes, int semRefugio800m) {
    }

    public record SetorDia(int dia, String setor, int pessoasNaLista, int contatadas, int pendentes,
                           int semRefugio800m, BigDecimal icEstimadoC) {
    }

    public record Resultado(List<Dia> dias, List<SetorDia> setores) {
    }

    /** Pessoa com a prioridade calculada (a prioridade não muda de um dia para outro). */
    private record Candidato(Pessoa pessoa, int pontos, Integer ipvs) {
    }

    public static Resultado executar(List<NivelDoDia> niveis, List<Pessoa> pessoas, Map<String, Setor> setores,
                                     int capacidadePorUbsDia, BigDecimal fatorK) {
        List<Candidato> ordenados = pessoas.stream()
                .filter(p -> setores.containsKey(p.setor()))
                .map(p -> {
                    Setor s = setores.get(p.setor());
                    int pontos = RegraPrioridade.calcular(p.idade(), p.moraSozinho(), p.mobilidadeReduzida(),
                            p.condicaoCronica(), s.ipvsGrupo(), s.notaCalor(), p.distanciaRefugioM()).pontos();
                    return new Candidato(p, pontos, s.ipvsGrupo());
                })
                .sorted(Comparator.comparingInt(Candidato::pontos).reversed()
                        .thenComparing(c -> c.pessoa().idade(), Comparator.reverseOrder())
                        .thenComparingLong(c -> c.pessoa().id())) // desempate igual ao da API de cadastro
                .toList();

        OptionalDouble media = setores.values().stream().map(Setor::lstC).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        Double lstMedia = media.isPresent() ? media.getAsDouble() : null;
        List<String> codigos = setores.keySet().stream().sorted().toList();

        List<Dia> dias = new ArrayList<>();
        List<SetorDia> porSetor = new ArrayList<>();
        List<Candidato> pendentes = new ArrayList<>();

        for (NivelDoDia nivel : niveis) {
            // fila do dia: pendentes de ontem na frente, depois a lista de hoje (sem repetir ninguém)
            List<Candidato> fila = new ArrayList<>(pendentes);
            Set<Long> naFila = new HashSet<>();
            pendentes.forEach(c -> naFila.add(c.pessoa().id()));
            for (Candidato c : ordenados) {
                if (AlcanceBuscaAtiva.entraNaLista(nivel.nivel(), c.pessoa().idade(), c.ipvs())
                        && naFila.add(c.pessoa().id())) {
                    fila.add(c);
                }
            }

            Map<String, Integer> usadosPorUbs = new HashMap<>();
            Map<String, int[]> contagem = new LinkedHashMap<>(); // setor -> [lista, contatadas, pendentes, semRefugio]
            List<Candidato> proximosPendentes = new ArrayList<>();
            int contatadas = 0;
            int semRefugio = 0;
            for (Candidato c : fila) {
                Pessoa p = c.pessoa();
                int[] n = contagem.computeIfAbsent(p.setor(), k -> new int[4]);
                n[0]++;
                int usados = usadosPorUbs.getOrDefault(p.ubs(), 0);
                if (usados < capacidadePorUbsDia) {
                    usadosPorUbs.put(p.ubs(), usados + 1);
                    n[1]++;
                    contatadas++;
                } else {
                    n[2]++;
                    proximosPendentes.add(c);
                }
                if (p.semRefugioProximo()) {
                    n[3]++;
                    semRefugio++;
                }
            }
            pendentes = proximosPendentes;
            dias.add(new Dia(nivel.dia(), fila.size(), contatadas, proximosPendentes.size(), semRefugio));

            for (String codigo : codigos) {
                int[] n = contagem.getOrDefault(codigo, new int[4]);
                porSetor.add(new SetorDia(nivel.dia(), codigo, n[0], n[1], n[2], n[3],
                        icEstimado(nivel.icMaxC(), setores.get(codigo).lstC(), lstMedia, fatorK)));
            }
        }
        return new Resultado(dias, porSetor);
    }

    static BigDecimal icEstimado(double icCidade, Double lstSetor, Double lstMedia, BigDecimal fatorK) {
        if (lstSetor == null || lstMedia == null) return null;
        double ic = icCidade + fatorK.doubleValue() * (lstSetor - lstMedia);
        return BigDecimal.valueOf(ic).setScale(2, RoundingMode.HALF_UP);
    }
}
