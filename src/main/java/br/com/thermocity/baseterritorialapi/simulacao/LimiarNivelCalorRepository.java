package br.com.thermocity.baseterritorialapi.simulacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LimiarNivelCalorRepository extends JpaRepository<LimiarNivelCalor, Long> {

    /** Regras da versão em uso, do nível mais baixo para o mais alto e, no mesmo nível, da regra de 1 dia primeiro. */
    List<LimiarNivelCalor> findByAtivoOrderByNivelAscDuracaoDiasAsc(Character ativo);
}
