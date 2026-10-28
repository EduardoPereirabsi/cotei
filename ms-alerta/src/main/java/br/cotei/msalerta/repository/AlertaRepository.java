package br.cotei.msalerta.repository;

import br.cotei.msalerta.domain.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByEmail(String email);

    List<Alerta> findByProdutoId(Long produtoId);

    /** Alertas ativos do produto cujo alvo foi atingido pelo valor informado. */
    List<Alerta> findByProdutoIdAndAtivoTrueAndPrecoAlvoGreaterThanEqual(Long produtoId, BigDecimal valor);
}
