package br.cotei.msloja.service;

public class ProdutoInexistenteException extends RuntimeException {

    public ProdutoInexistenteException(Long produtoId) {
        super("Produto " + produtoId + " nao existe no catalogo");
    }
}
