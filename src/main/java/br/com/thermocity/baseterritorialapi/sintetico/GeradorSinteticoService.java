package br.com.thermocity.baseterritorialapi.sintetico;

import br.com.thermocity.baseterritorialapi.common.RequisicaoInvalidaException;
import br.com.thermocity.baseterritorialapi.geo.GeoService;
import br.com.thermocity.baseterritorialapi.servico.Servico;
import br.com.thermocity.baseterritorialapi.servico.ServicoService;
import br.com.thermocity.baseterritorialapi.setor.Setor;
import br.com.thermocity.baseterritorialapi.setor.SetorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gera um cadastro FICTÍCIO de pessoas 60+ distribuído pelos setores na mesma proporção do Censo 2022.
 *
 * <ol>
 *   <li>Divide a quantidade pedida entre os setores proporcionalmente à população 60+ (método dos maiores restos).</li>
 *   <li>Dentro de cada setor, divide entre 60–69 e 70+ na proporção do Censo do próprio setor.</li>
 *   <li>Sorteia idade, sexo, ponto dentro do setor e atributos de vulnerabilidade com as taxas PROVISÓRIAS
 *       de {@link SinteticoProperties}.</li>
 *   <li>Associa a UBS mais próxima do ponto sorteado.</li>
 * </ol>
 * A mesma semente sempre gera o mesmo cadastro. Uma nova geração substitui a anterior.
 */
@Service
public class GeradorSinteticoService {

    static final String AVISO = "Dados fictícios para teste. Taxas de vulnerabilidade PROVISÓRIAS, não são estatística oficial.";

    private static final String[] NOMES_F = {"Maria", "Ana", "Francisca", "Antônia", "Adriana", "Juliana", "Márcia",
            "Fernanda", "Aparecida", "Sandra", "Cleusa", "Lourdes", "Neide", "Tereza", "Rosa", "Helena", "Irene",
            "Dirce", "Zilda", "Benedita"};
    private static final String[] NOMES_M = {"José", "João", "Antônio", "Francisco", "Carlos", "Paulo", "Pedro",
            "Luiz", "Sebastião", "Manoel", "Geraldo", "Valdir", "Osvaldo", "Benedito", "Jorge", "Roberto", "Mário",
            "Joaquim", "Waldemar", "Aparecido"};
    private static final String[] SOBRENOMES = {"Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira",
            "Alves", "Pereira", "Lima", "Gomes", "Ribeiro", "Carvalho", "Almeida", "Lopes", "Martins", "Rocha",
            "Barbosa", "Nascimento", "Moreira", "Teixeira"};

    private final SetorRepository setores;
    private final PessoaSinteticaRepository pessoas;
    private final ServicoService servicos;
    private final GeoService geo;
    private final SinteticoProperties props;

    public GeradorSinteticoService(SetorRepository setores, PessoaSinteticaRepository pessoas,
                                   ServicoService servicos, GeoService geo, SinteticoProperties props) {
        this.setores = setores;
        this.pessoas = pessoas;
        this.servicos = servicos;
        this.geo = geo;
        this.props = props;
    }

    @Transactional
    public GeracaoResultado gerar(int quantidade, Long sementeInformada) {
        long semente = sementeInformada != null ? sementeInformada : props.sementePadrao();
        Random aleatorio = new Random(semente);

        List<Setor> base = setores.findAll().stream()
                .filter(s -> s.getPop60mais() != null && s.getPop60mais() > 0)
                .sorted(Comparator.comparing(Setor::getCodigo))
                .toList();
        if (base.isEmpty()) {
            throw new RequisicaoInvalidaException("Não há setores com população 60+ carregada. Importe os setores primeiro.");
        }

        // As UBS são lidas uma vez só; a mais próxima de cada pessoa é calculada em memória.
        // (Antes era uma consulta ao banco por pessoa: com milhares de pessoas, o pedido estourava o tempo limite.)
        List<Servico> ubs = servicos.listarUbs().stream().filter(Servico::temCoordenada).toList();

        int[] porSetor = maioresRestos(quantidade, base.stream().mapToDouble(Setor::getPop60mais).toArray());
        OffsetDateTime agora = OffsetDateTime.now();
        List<PessoaSintetica> geradas = new ArrayList<>(quantidade);

        for (int i = 0; i < base.size(); i++) {
            int n = porSetor[i];
            if (n == 0) continue;
            Setor s = base.get(i);
            int p6069 = valor(s.getPop60a69());
            int p70 = valor(s.getPop70mais());
            int[] faixas = maioresRestos(n, new double[]{p6069, p70});
            for (int k = 0; k < n; k++) {
                boolean setentaMais = k >= faixas[0];
                geradas.add(novaPessoa(s, setentaMais, aleatorio, semente, agora, ubs));
            }
        }

        pessoas.deleteAllInBatch();
        pessoas.saveAll(geradas);

        return new GeracaoResultado(
                geradas.size(), semente,
                (int) geradas.stream().map(PessoaSintetica::getSetorCodigo).distinct().count(),
                contar(geradas, PessoaSintetica::getFaixaEtaria),
                contar(geradas, PessoaSintetica::getSexo),
                geradas.stream().filter(PessoaSintetica::isMoraSozinho).count(),
                geradas.stream().filter(PessoaSintetica::isMobilidadeReduzida).count(),
                geradas.stream().filter(PessoaSintetica::isCondicaoCronica).count(),
                AVISO);
    }

    private PessoaSintetica novaPessoa(Setor s, boolean setentaMais, Random r, long semente, OffsetDateTime agora,
                                       List<Servico> ubs) {
        PessoaSintetica p = new PessoaSintetica();
        boolean feminino = r.nextDouble() < props.probabilidadeFeminino();
        p.setSexo(feminino ? "F" : "M");
        p.setNome((feminino ? sorteia(NOMES_F, r) : sorteia(NOMES_M, r)) + " " + sorteia(SOBRENOMES, r) + " " + sorteia(SOBRENOMES, r));
        p.setIdade(setentaMais ? idade70mais(r) : 60 + r.nextInt(10));
        p.setFaixaEtaria(setentaMais ? "70+" : "60-69");
        p.setSetorCodigo(s.getCodigo());

        double[] ponto = geo.pontoAleatorio(s.getCodigo(), r);
        p.setLat(arredonda(ponto[0]));
        p.setLon(arredonda(ponto[1]));

        p.setMoraSozinho(r.nextDouble() < props.taxaMoraSozinho());
        p.setMobilidadeReduzida(r.nextDouble() < props.taxaMobilidadeReduzida());
        p.setCondicaoCronica(r.nextDouble() < props.taxaCondicaoCronica());

        // Telefones claramente fictícios (prefixo 0000), nunca discáveis.
        p.setTelefone(telefoneFicticio(r));
        if (!p.isMoraSozinho() || r.nextDouble() < 0.5) {
            p.setContatoReferenciaNome(sorteia(r.nextBoolean() ? NOMES_F : NOMES_M, r) + " " + sorteia(SOBRENOMES, r));
            p.setContatoReferenciaTelefone(telefoneFicticio(r));
        }

        ubs.stream()
                .min(Comparator.comparingDouble(u -> GeoService.distanciaMetros(p.getLat(), p.getLon(), u.getLat(), u.getLon())))
                .ifPresent(u -> {
                    p.setUbsReferenciaId(u.getId());
                    p.setUbsReferenciaNome(u.getNome());
                });

        p.setConsentimentoLgpd(true);
        p.setConsentimentoEm(agora);
        p.setSintetico(true);
        p.setSemente(semente);
        p.setGeradoEm(agora);
        return p;
    }

    /**
     * Idade entre 70 e 95 com peso decrescente (quanto mais velho, menos provável).
     * Simplificação PROVISÓRIA: o Censo 2022 por setor não traz faixas acima de 70.
     */
    private static int idade70mais(Random r) {
        int total = 0;
        for (int idade = 70; idade <= 95; idade++) total += 96 - idade;
        int sorteio = r.nextInt(total);
        for (int idade = 70; idade <= 95; idade++) {
            sorteio -= 96 - idade;
            if (sorteio < 0) return idade;
        }
        return 95;
    }

    /** Distribui {@code total} unidades proporcionalmente aos pesos, sem perder nenhuma (maiores restos). */
    static int[] maioresRestos(int total, double[] pesos) {
        double soma = 0;
        for (double p : pesos) soma += p;
        int[] resultado = new int[pesos.length];
        if (soma <= 0) {
            resultado[0] = total;
            return resultado;
        }
        double[] restos = new double[pesos.length];
        int distribuido = 0;
        for (int i = 0; i < pesos.length; i++) {
            double cota = total * pesos[i] / soma;
            resultado[i] = (int) Math.floor(cota);
            restos[i] = cota - resultado[i];
            distribuido += resultado[i];
        }
        Integer[] ordem = new Integer[pesos.length];
        for (int i = 0; i < ordem.length; i++) ordem[i] = i;
        java.util.Arrays.sort(ordem, (a, b) -> Double.compare(restos[b], restos[a]) != 0
                ? Double.compare(restos[b], restos[a]) : Integer.compare(a, b));
        for (int i = 0; i < total - distribuido; i++) resultado[ordem[i]]++;
        return resultado;
    }

    private static String telefoneFicticio(Random r) {
        return String.format("+55 18 0000-%04d", r.nextInt(10000));
    }

    private static String sorteia(String[] opcoes, Random r) {
        return opcoes[r.nextInt(opcoes.length)];
    }

    private static int valor(Integer v) {
        return v == null ? 0 : v;
    }

    private static double arredonda(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }

    private static Map<String, Long> contar(List<PessoaSintetica> lista, Function<PessoaSintetica, String> chave) {
        return lista.stream().collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.counting()));
    }
}
