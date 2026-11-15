package br.com.thermocity.baseterritorialapi.simulacao;

import java.time.LocalDate;
import java.util.List;

/** Nível de calor de cada dia do cenário e a versão dos limiares usada no cálculo. */
public record NiveisSimulacaoResponse(String versaoLimiar, List<Dia> dias) {

    public record Dia(int dia, LocalDate data, double icMaxC, int nivel, String nome, String regra, String acaoEsperada) {
    }

    static NiveisSimulacaoResponse de(NivelCalorCalculadora.Resultado resultado, LocalDate dataInicio) {
        return new NiveisSimulacaoResponse(resultado.versaoLimiar(), resultado.dias().stream()
                .map(d -> new Dia(d.dia(), dataInicio == null ? null : dataInicio.plusDays(d.dia() - 1L),
                        d.icMaxC(), d.nivel(), d.nome(), d.regra(), d.acaoEsperada()))
                .toList());
    }
}
