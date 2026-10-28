package br.cotei.msalerta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * "Me avise quando este produto custar ate X."
 * Disparo unico: ao ser atingido, o alerta e desativado.
 */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoAlvo;

    private boolean ativo = true;

    private Instant criadoEm = Instant.now();

    private Instant disparadoEm;

    public Alerta() {
    }

    public Alerta(Long produtoId, String email, BigDecimal precoAlvo) {
        this.produtoId = produtoId;
        this.email = email;
        this.precoAlvo = precoAlvo;
    }

    public void disparar() {
        this.ativo = false;
        this.disparadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Long getProdutoId() { return produtoId; }
    public String getEmail() { return email; }
    public BigDecimal getPrecoAlvo() { return precoAlvo; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getDisparadoEm() { return disparadoEm; }
}
