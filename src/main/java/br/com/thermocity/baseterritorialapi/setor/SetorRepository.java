package br.com.thermocity.baseterritorialapi.setor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorRepository extends JpaRepository<Setor, String> {

    /** Existe algum setor neste distrito? (recorte da simulação) */
    boolean existsByNmDistritoIgnoreCase(String nmDistrito);
}
