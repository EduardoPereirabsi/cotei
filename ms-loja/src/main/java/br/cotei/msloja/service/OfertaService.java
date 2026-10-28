package br.cotei.msloja.service;

import br.cotei.msloja.client.ProdutoClient;
import br.cotei.msloja.domain.Oferta;
import br.cotei.msloja.repository.OfertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class OfertaService {

    private final OfertaRepository repository;
    private final ProdutoClient produtoClient;

    public OfertaService(OfertaRepository repository, ProdutoClient produtoClient) {
        this.repository = repository;
        this.produtoClient = produtoClient;
    }

    public List<Oferta> listar(Long produtoId) {
        return produtoId == null ? repository.findAll() : repository.findByProdutoId(produtoId);
    }

    public Optional<Oferta> buscar(Long id) {
        return repository.findById(id);
    }

    /** Menor valor final (preco + frete) entre as ofertas disponiveis do produto. */
    public Optional<Oferta> melhorOferta(Long produtoId) {
        return repository.findByProdutoIdAndDisponivelTrue(produtoId).stream()
                .min(Comparator.comparing(Oferta::getPrecoTotal));
    }

    /**
     * Registra ou atualiza a oferta de uma loja para um produto.
     * Usado tanto pela API REST quanto pelo consumidor do evento oferta.atualizada.
     *
     * @throws ProdutoInexistenteException se o ms-produto nao conhece o produto
     */
    @Transactional
    public Oferta registrar(Long produtoId, String loja, BigDecimal preco, BigDecimal frete,
                            boolean disponivel, String url, Instant coletadoEm) {
        if (!produtoClient.existe(produtoId)) {
            throw new ProdutoInexistenteException(produtoId);
        }
        Oferta oferta = repository.findByProdutoIdAndLoja(produtoId, loja).orElseGet(Oferta::new);
        oferta.setProdutoId(produtoId);
        oferta.setLoja(loja);
        oferta.setPreco(preco);
        oferta.setFrete(frete == null ? BigDecimal.ZERO : frete);
        oferta.setDisponivel(disponivel);
        oferta.setUrl(url);
        oferta.setColetadoEm(coletadoEm == null ? Instant.now() : coletadoEm);
        return repository.save(oferta);
    }

    public boolean deletar(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
