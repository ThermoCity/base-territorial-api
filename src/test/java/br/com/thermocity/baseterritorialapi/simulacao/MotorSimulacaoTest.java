package br.com.thermocity.baseterritorialapi.simulacao;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Motor dia a dia: lista do dia, fila de contatos, refúgio a 800 m e IC estimado com o fator k. */
class MotorSimulacaoTest {

    private static final BigDecimal K = new BigDecimal("0.25");

    private static List<NivelDoDia> niveis(int... nc) {
        List<NivelDoDia> dias = new ArrayList<>();
        for (int i = 0; i < nc.length; i++) dias.add(new NivelDoDia(i + 1, 36.0, nc[i], "NC" + nc[i], "r", "a"));
        return dias;
    }

    /** n pessoas de 75 anos no mesmo setor (IPVS 5) e na mesma UBS, com refúgio a distanciaM. */
    private static List<MotorSimulacao.Pessoa> pessoas(int n, Double distanciaM) {
        List<MotorSimulacao.Pessoa> lista = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            lista.add(new MotorSimulacao.Pessoa(i, 75, "S1", "UBS-1", false, false, false, distanciaM));
        }
        return lista;
    }

    private static final Map<String, MotorSimulacao.Setor> UM_SETOR =
            Map.of("S1", new MotorSimulacao.Setor("S1", 5, 5, 40.0));

    @Test
    void niveisVemDoCalculoComPersistencia() {
        // 35,4 / 35,8 / 35,2 °C: o dia 3 fica em NC3 pela regra de 3 dias seguidos
        List<NivelDoDia> n = NivelCalorCalculadora.classificar(LimiaresDeTeste.todos(), List.of(35.4, 35.8, 35.2), null);
        assertThat(n).extracting(NivelDoDia::nivel).containsExactly(2, 2, 3);

        MotorSimulacao.Resultado r = MotorSimulacao.executar(n, pessoas(10, 300.0), UM_SETOR, 80, K);
        // NC2 não tem busca ativa; no NC3 entram os 70+ de IPVS 5–6
        assertThat(r.dias()).extracting(MotorSimulacao.Dia::pessoasNaLista).containsExactly(0, 0, 10);
    }

    @Test
    void quemFicaPendenteEntraNaFrenteDaFilaDoDiaSeguinte() {
        // 120 pessoas na lista, capacidade de 80 contatos por dia
        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis(4, 4), pessoas(120, 300.0), UM_SETOR, 80, K);

        MotorSimulacao.Dia dia1 = r.dias().get(0);
        assertThat(dia1.pessoasNaLista()).isEqualTo(120);
        assertThat(dia1.contatadas()).isEqualTo(80);
        assertThat(dia1.pendentes()).isEqualTo(40);
        // dia 2: os 40 pendentes vão primeiro e cabem na capacidade; ninguém aparece duas vezes
        MotorSimulacao.Dia dia2 = r.dias().get(1);
        assertThat(dia2.pessoasNaLista()).isEqualTo(120);
        assertThat(dia2.contatadas()).isEqualTo(80);
        assertThat(dia2.pendentes()).isEqualTo(40);
    }

    @Test
    void pendentesDoDia1SaoOsPrimeirosContatadosNoDia2() {
        // capacidade 2: no dia 1 os 2 de maior prioridade; no dia 2 os 2 que ficaram pendentes vêm primeiro
        List<MotorSimulacao.Pessoa> lista = List.of(
                new MotorSimulacao.Pessoa(1, 85, "S1", "UBS-1", true, true, false, 300.0),
                new MotorSimulacao.Pessoa(2, 80, "S1", "UBS-1", true, false, false, 300.0),
                new MotorSimulacao.Pessoa(3, 72, "S1", "UBS-1", false, false, false, 300.0),
                new MotorSimulacao.Pessoa(4, 71, "S1", "UBS-1", false, false, false, 300.0));
        // dia 2 em NC3 de novo: a lista seria a mesma, mas os pendentes (3 e 4) passam na frente
        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis(3, 3, 3), lista, UM_SETOR, 2, K);

        assertThat(r.dias()).extracting(MotorSimulacao.Dia::contatadas).containsExactly(2, 2, 2);
        assertThat(r.dias()).extracting(MotorSimulacao.Dia::pendentes).containsExactly(2, 2, 2);
    }

    @Test
    void pendentesContinuamNaFilaMesmoSeONivelCair() {
        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis(4, 1), pessoas(10, 300.0), UM_SETOR, 6, K);

        assertThat(r.dias().get(0).pendentes()).isEqualTo(4);
        assertThat(r.dias().get(1).pessoasNaLista()).isEqualTo(4);
        assertThat(r.dias().get(1).contatadas()).isEqualTo(4);
        assertThat(r.dias().get(1).pendentes()).isZero();
    }

    @Test
    void capacidadeEPorUbs() {
        List<MotorSimulacao.Pessoa> lista = new ArrayList<>(pessoas(5, 300.0));
        for (int i = 6; i <= 10; i++) lista.add(new MotorSimulacao.Pessoa(i, 75, "S1", "UBS-2", false, false, false, 300.0));

        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis(4), lista, UM_SETOR, 3, K);

        assertThat(r.dias().get(0).contatadas()).isEqualTo(6); // 3 em cada UBS
        assertThat(r.dias().get(0).pendentes()).isEqualTo(4);
    }

    @Test
    void semRefugiosTodosDaListaFicamSemRefugioProximo() {
        MotorSimulacao.Resultado r = MotorSimulacao.executar(niveis(5, 5), pessoas(30, null), UM_SETOR, 10, K);

        assertThat(r.dias()).allSatisfy(d -> assertThat(d.semRefugio800m()).isEqualTo(d.pessoasNaLista()));
        assertThat(r.dias().get(0).semRefugio800m()).isEqualTo(30);
    }

    @Test
    void refugioAte800mNaoConta() {
        List<MotorSimulacao.Pessoa> lista = List.of(
                new MotorSimulacao.Pessoa(1, 75, "S1", "UBS-1", false, false, false, 800.0),
                new MotorSimulacao.Pessoa(2, 75, "S1", "UBS-1", false, false, false, 801.0));

        assertThat(MotorSimulacao.executar(niveis(5), lista, UM_SETOR, 10, K).dias().get(0).semRefugio800m()).isEqualTo(1);
    }

    @Test
    void resultadoPorSetorEIcEstimadoComFatorK() {
        Map<String, MotorSimulacao.Setor> setores = Map.of(
                "A", new MotorSimulacao.Setor("A", 5, 5, 44.0),
                "B", new MotorSimulacao.Setor("B", 2, 1, 36.0),
                "C", new MotorSimulacao.Setor("C", 3, null, null));
        List<MotorSimulacao.Pessoa> lista = List.of(
                new MotorSimulacao.Pessoa(1, 75, "A", "U", false, false, false, 2000.0),
                new MotorSimulacao.Pessoa(2, 75, "B", "U", false, false, false, 100.0));
        List<NivelDoDia> um = List.of(new NivelDoDia(1, 37.0, 5, "Emergência", "r", "a"));

        MotorSimulacao.Resultado r = MotorSimulacao.executar(um, lista, setores, 10, K);

        assertThat(r.setores()).hasSize(3);
        MotorSimulacao.SetorDia a = r.setores().get(0);
        assertThat(a.setor()).isEqualTo("A");
        assertThat(a.pessoasNaLista()).isEqualTo(1);
        assertThat(a.semRefugio800m()).isEqualTo(1);
        // média de superfície = 40 °C; A: 37 + 0,25 × (44 − 40) = 38,00; B: 37 + 0,25 × (36 − 40) = 36,00
        assertThat(a.icEstimadoC()).isEqualByComparingTo("38.00");
        assertThat(r.setores().get(1).icEstimadoC()).isEqualByComparingTo("36.00");
        assertThat(r.setores().get(2).icEstimadoC()).isNull();
        assertThat(r.setores().get(2).pessoasNaLista()).isZero();
    }

    @Test
    void mesmoCenarioMesmoResultado() {
        List<MotorSimulacao.Pessoa> lista = pessoas(50, 900.0);
        assertThat(MotorSimulacao.executar(niveis(3, 4, 5), lista, UM_SETOR, 7, K))
                .isEqualTo(MotorSimulacao.executar(niveis(3, 4, 5), lista, UM_SETOR, 7, K));
    }
}
