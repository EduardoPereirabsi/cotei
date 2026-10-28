package br.cotei.msloja.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Comunicacao sincrona (REST) com o ms-produto, fonte da verdade do catalogo.
 * Em Kubernetes a URL usa o nome do Service: http://ms-produto:8080/produto
 */
@Component
public class ProdutoClient {

    private final RestClient rest;

    public ProdutoClient(@Value("${cotei.produto.url}") String baseUrl) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeouts)
                .build();
    }

    /**
     * @return true se o produto existe, false se o ms-produto respondeu 404
     * @throws ProdutoIndisponivelException se nao foi possivel consultar o ms-produto
     */
    public boolean existe(Long produtoId) {
        try {
            rest.get().uri("/produtos/{id}", produtoId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ProdutoIndisponivelException(e);
        }
    }
}
