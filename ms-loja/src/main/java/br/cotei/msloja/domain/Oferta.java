package br.cotei.msloja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Oferta de um produto em uma loja parceira.
 * A mesma loja so tem uma oferta por produto: uma nova coleta atualiza a existente.
 */
@Entity
@Table(name = "ofertas", uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "loja"}))
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(nullable = false)
    private String loja;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal frete = BigDecimal.ZERO;

    private boolean disponivel;

    private String url;

    private Instant coletadoEm;

    public Oferta() {
    }

    /** Valor que o consumidor efetivamente paga: e o criterio da melhor oferta. */
    public BigDecimal getPrecoTotal() {
        return preco.add(frete == null ? BigDecimal.ZERO : frete);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
    public String getLoja() { return loja; }
    public void setLoja(String loja) { this.loja = loja; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public BigDecimal getFrete() { return frete; }
    public void setFrete(BigDecimal frete) { this.frete = frete; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getColetadoEm() { return coletadoEm; }
    public void setColetadoEm(Instant coletadoEm) { this.coletadoEm = coletadoEm; }
}
