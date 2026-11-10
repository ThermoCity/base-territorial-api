package br.com.thermocity.baseterritorialapi.servico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, String> {

    List<Servico> findByTipoAndCoordStatus(TipoServico tipo, StatusCoordenada coordStatus);
}
