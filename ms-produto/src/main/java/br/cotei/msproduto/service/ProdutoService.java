package br.cotei.msproduto.service;

import br.cotei.msproduto.domain.Produto;
import br.cotei.msproduto.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscar(Long id) {
        return produtoRepository.findById(id);
    }

    public Produto salvar(Produto produto) {
        produto.setId(null);
        return produtoRepository.save(produto);
    }

    public Optional<Produto> atualizar(Long id, Produto dados) {
        return produtoRepository.findById(id).map(atual -> {
            atual.setNome(dados.getNome());
            atual.setMarca(dados.getMarca());
            atual.setCategoria(dados.getCategoria());
            atual.setEan(dados.getEan());
            atual.setDescricao(dados.getDescricao());
            atual.setAtivo(dados.isAtivo());
            return produtoRepository.save(atual);
        });
    }

    public boolean deletar(Long id) {
        if (!produtoRepository.existsById(id)) {
            return false;
        }
        produtoRepository.deleteById(id);
        return true;
    }
}
