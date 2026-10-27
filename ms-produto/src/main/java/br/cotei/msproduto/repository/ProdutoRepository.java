package br.cotei.msproduto.repository;

import br.cotei.msproduto.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
