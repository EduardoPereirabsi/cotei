package br.cotei.msalerta.client;

public class ProdutoIndisponivelException extends RuntimeException {

    public ProdutoIndisponivelException(Throwable causa) {
        super("Nao foi possivel consultar o ms-produto", causa);
    }
}
