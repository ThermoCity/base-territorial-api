package br.com.thermocity.baseterritorialapi.sintetico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PessoaSinteticaRepository extends JpaRepository<PessoaSintetica, Long> {

    List<PessoaSintetica> findBySetorCodigoOrderByIdAsc(String setorCodigo);
}
