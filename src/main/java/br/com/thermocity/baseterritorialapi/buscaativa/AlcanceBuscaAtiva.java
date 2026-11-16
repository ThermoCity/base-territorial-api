package br.com.thermocity.baseterritorialapi.buscaativa;

/**
 * Quem entra na lista do dia em cada nível de calor. É a "ação esperada" gravada em
 * TMC_LIMIAR_NIVEL_CALOR (versão A707-2003-2026-v1), escrita como regra:
 * <ul>
 *   <li>NC1 e NC2: sem busca ativa (lista vazia);</li>
 *   <li>NC3: idosos com 70+ anos nos setores de IPVS 5 e 6;</li>
 *   <li>NC4: todos os 60+ nos setores de IPVS 4 a 6;</li>
 *   <li>NC5: todos os 60+ da cidade.</li>
 * </ul>
 * Setor sem classificação no IPVS só entra no NC5. Vale para o modo real e para a simulação.
 */
public final class AlcanceBuscaAtiva {

    public static final int NIVEL_INICIO_BUSCA = 3;

    private AlcanceBuscaAtiva() {
    }

    public static boolean entraNaLista(int nivel, int idade, Integer ipvsGrupo) {
        if (idade < 60) return false;
        return switch (nivel) {
            case 3 -> idade >= 70 && ipvsGrupo != null && ipvsGrupo >= 5;
            case 4 -> ipvsGrupo != null && ipvsGrupo >= 4;
            case 5 -> true;
            default -> false;
        };
    }
}
