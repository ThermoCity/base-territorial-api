package br.com.thermocity.baseterritorialapi.simulacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulacaoDiaRepository extends JpaRepository<SimulacaoDia, SimulacaoDiaId> {

    List<SimulacaoDia> findByIdIdSimulacaoOrderByIdDia(Long idSimulacao);
}
