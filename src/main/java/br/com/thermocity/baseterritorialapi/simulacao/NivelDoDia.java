package br.com.thermocity.baseterritorialapi.simulacao;

/**
 * Nível de calor calculado para um dia da simulação.
 *
 * @param dia          posição do dia no cenário (1 = primeiro dia)
 * @param icMaxC       índice de calor máximo do dia, em °C
 * @param nivel        1 a 5
 * @param nome         Normal, Atenção, Alerta, Alto risco ou Emergência
 * @param regra        a regra que definiu o nível (texto da tabela de limiares)
 * @param acaoEsperada o que a rede de saúde deve fazer nesse nível
 */
public record NivelDoDia(int dia, double icMaxC, int nivel, String nome, String regra, String acaoEsperada) {
}
