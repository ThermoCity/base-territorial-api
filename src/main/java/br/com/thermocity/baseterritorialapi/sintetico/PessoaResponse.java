package br.com.thermocity.baseterritorialapi.sintetico;

import java.time.OffsetDateTime;

public record PessoaResponse(
        Long id,
        String nome,
        Integer idade,
        String faixaEtaria,
        String sexo,
        String setorCodigo,
        Double lat,
        Double lon,
        boolean moraSozinho,
        boolean mobilidadeReduzida,
        boolean condicaoCronica,
        String telefone,
        String contatoReferenciaNome,
        String contatoReferenciaTelefone,
        String ubsReferenciaId,
        String ubsReferenciaNome,
        boolean consentimentoLgpd,
        boolean sintetico,
        Long semente,
        OffsetDateTime geradoEm) {

    public static PessoaResponse de(PessoaSintetica p) {
        return new PessoaResponse(p.getId(), p.getNome(), p.getIdade(), p.getFaixaEtaria(), p.getSexo(),
                p.getSetorCodigo(), p.getLat(), p.getLon(), p.isMoraSozinho(), p.isMobilidadeReduzida(),
                p.isCondicaoCronica(), p.getTelefone(), p.getContatoReferenciaNome(),
                p.getContatoReferenciaTelefone(), p.getUbsReferenciaId(), p.getUbsReferenciaNome(),
                p.isConsentimentoLgpd(), p.isSintetico(), p.getSemente(), p.getGeradoEm());
    }
}
