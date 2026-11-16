package br.com.thermocity.baseterritorialapi.simulacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ResumoDiarioClimaRepository extends JpaRepository<ResumoDiarioClima, ResumoDiarioClimaId> {

    /** IC máximo de cada dia válido da série, em ordem de data. */
    @Query("""
            select new br.com.thermocity.baseterritorialapi.simulacao.DiaClima(r.id.dataLocal, r.icMaxC)
              from ResumoDiarioClima r
             where r.id.fonte = :fonte and r.id.tipo = :tipo and r.diaValido = :valido and r.icMaxC is not null
             order by r.id.dataLocal""")
    List<DiaClima> serieDiaria(@Param("fonte") String fonte, @Param("tipo") String tipo, @Param("valido") Character valido);

    /** Maior IC máximo diário da série (acima dele, um cenário é extrapolação). */
    @Query("""
            select max(r.icMaxC) from ResumoDiarioClima r
             where r.id.fonte = :fonte and r.id.tipo = :tipo and r.diaValido = :valido""")
    BigDecimal maxIcMax(@Param("fonte") String fonte, @Param("tipo") String tipo, @Param("valido") Character valido);
}
