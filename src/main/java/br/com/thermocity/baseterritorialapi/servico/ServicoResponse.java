package br.com.thermocity.baseterritorialapi.servico;

/** Um serviço (UBS ou refúgio) com a distância em metros até o ponto consultado. */
public record ServicoResponse(
        String id,
        String nome,
        TipoServico tipo,
        String categoria,
        String endereco,
        String bairro,
        Double lat,
        Double lon,
        String setorCodigo,
        String fonte,
        String revisao,
        Integer distanciaMetros) {

    public static ServicoResponse de(Servico s, Double distancia) {
        return new ServicoResponse(s.getId(), s.getNome(), s.getTipo(), s.getCategoria(), s.getEndereco(),
                s.getBairro(), s.getLat(), s.getLon(), s.getSetorCodigo(), s.getFonte(), s.getRevisao(),
                distancia == null ? null : (int) Math.round(distancia));
    }
}
