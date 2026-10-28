package br.cotei.msalerta.service;

import br.cotei.msalerta.client.ProdutoClient;
import br.cotei.msalerta.client.ProdutoIndisponivelException;
import br.cotei.msalerta.domain.Alerta;
import br.cotei.msalerta.domain.Notificacao;
import br.cotei.msalerta.repository.AlertaRepository;
import br.cotei.msalerta.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertaService {

    private final AlertaRepository alertas;
    private final NotificacaoRepository notificacoes;
    private final ProdutoClient produtoClient;

    public AlertaService(AlertaRepository alertas, NotificacaoRepository notificacoes, ProdutoClient produtoClient) {
        this.alertas = alertas;
        this.notificacoes = notificacoes;
        this.produtoClient = produtoClient;
    }

    /** @throws ProdutoInexistenteException se o ms-produto nao conhece o produto */
    @Transactional
    public Alerta criar(Long produtoId, String email, BigDecimal precoAlvo) {
        if (!produtoClient.existe(produtoId)) {
            throw new ProdutoInexistenteException(produtoId);
        }
        return alertas.save(new Alerta(produtoId, email, precoAlvo));
    }

    public List<Alerta> listar(String email, Long produtoId) {
        if (email != null) return alertas.findByEmail(email);
        if (produtoId != null) return alertas.findByProdutoId(produtoId);
        return alertas.findAll();
    }

    public Optional<Alerta> buscar(Long id) {
        return alertas.findById(id);
    }

    public boolean remover(Long id) {
        if (!alertas.existsById(id)) return false;
        alertas.deleteById(id);
        return true;
    }

    public List<Notificacao> notificacoes(String email) {
        return email == null ? notificacoes.findAllByOrderByCriadaEmDesc() : notificacoes.findByEmailOrderByCriadaEmDesc(email);
    }

    /**
     * Confere uma oferta contra os alertas ativos do produto.
     * Compara o valor total (preco + frete); ofertas indisponiveis nao disparam.
     */
    @Transactional
    public List<Notificacao> processarOferta(Long produtoId, String loja, BigDecimal preco, BigDecimal frete,
                                             boolean disponivel, String url) {
        if (!disponivel) {
            return List.of();
        }
        BigDecimal total = preco.add(frete == null ? BigDecimal.ZERO : frete);
        List<Alerta> atingidos = alertas.findByProdutoIdAndAtivoTrueAndPrecoAlvoGreaterThanEqual(produtoId, total);
        if (atingidos.isEmpty()) {
            return List.of();
        }

        // Se o ms-produto estiver fora do ar, o alerta dispara mesmo assim com nome generico:
        // perder a notificacao seria pior do que envia-la com o nome incompleto.
        String nome;
        try {
            nome = produtoClient.buscarNome(produtoId).orElse("Produto " + produtoId);
        } catch (ProdutoIndisponivelException e) {
            nome = "Produto " + produtoId;
        }

        List<Notificacao> geradas = new ArrayList<>();
        for (Alerta alerta : atingidos) {
            alerta.disparar();
            geradas.add(notificacoes.save(new Notificacao(alerta, nome, loja, total, url)));
        }
        return geradas;
    }
}
