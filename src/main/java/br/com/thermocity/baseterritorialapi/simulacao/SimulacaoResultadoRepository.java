package br.com.thermocity.baseterritorialapi.simulacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulacaoResultadoRepository extends JpaRepository<SimulacaoResultado, SimulacaoResultadoId> {

    List<SimulacaoResultado> findByIdIdSimulacaoOrderByIdDiaAscIdCodigoSetorAsc(Long idSimulacao);

    List<SimulacaoResultado> findByIdIdSimulacaoAndIdDiaOrderByIdCodigoSetor(Long idSimulacao, Short dia);
}
