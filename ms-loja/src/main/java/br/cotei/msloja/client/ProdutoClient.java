package br.cotei.msloja.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

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
