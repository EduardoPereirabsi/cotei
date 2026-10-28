package br.cotei.msalerta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Aviso gerado quando uma oferta atinge o preco-alvo de um alerta. */
@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long alertaId;

    @Column(nullable = false)
    private String email;

    private Long produtoId;

    private String produtoNome;

    private String loja;

    @Column(precision = 12, scale = 2)
    private BigDecimal precoTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal precoAlvo;

    private String url;

    private Instant criadaEm = Instant.now();

    public Notificacao() {
    }

    public Notificacao(Alerta alerta, String produtoNome, String loja, BigDecimal precoTotal, String url) {
        this.alertaId = alerta.getId();
        this.email = alerta.getEmail();
        this.produtoId = alerta.getProdutoId();
        this.produtoNome = produtoNome;
        this.loja = loja;
        this.precoTotal = precoTotal;
        this.precoAlvo = alerta.getPrecoAlvo();
        this.url = url;
    }

    /** Texto que seria enviado ao usuario (o envio e simulado no log). */
    public String getMensagem() {
        return "%s saiu por R$ %s na %s (seu alvo: R$ %s)".formatted(produtoNome, precoTotal, loja, precoAlvo);
    }

    public Long getId() { return id; }
    public Long getAlertaId() { return alertaId; }
    public String getEmail() { return email; }
    public Long getProdutoId() { return produtoId; }
    public String getProdutoNome() { return produtoNome; }
    public String getLoja() { return loja; }
    public BigDecimal getPrecoTotal() { return precoTotal; }
    public BigDecimal getPrecoAlvo() { return precoAlvo; }
    public String getUrl() { return url; }
    public Instant getCriadaEm() { return criadaEm; }
}
