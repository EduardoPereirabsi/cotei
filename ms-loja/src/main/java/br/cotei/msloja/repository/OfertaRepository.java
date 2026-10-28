package br.cotei.msloja.repository;

import br.cotei.msloja.domain.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByProdutoId(Long produtoId);

    List<Oferta> findByProdutoIdAndDisponivelTrue(Long produtoId);

    Optional<Oferta> findByProdutoIdAndLoja(Long produtoId, String loja);
}
