package br.cotei.msalerta.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
public class ProdutoClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProdutoResumo(Long id, String nome) {
    }

    private final RestClient rest;
    private final JsonMapper json;

    public ProdutoClient(@Value("${cotei.produto.url}") String baseUrl, JsonMapper json) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts).build();
        this.json = json;
    }

    /**
     * @return nome do produto, ou vazio se o ms-produto respondeu 404
     * @throws ProdutoIndisponivelException se nao foi possivel consultar o ms-produto
     */
    public Optional<String> buscarNome(Long produtoId) {
        try {
            String corpo = rest.get().uri("/produtos/{id}", produtoId).retrieve().body(String.class);
            return Optional.ofNullable(json.readValue(corpo, ProdutoResumo.class).nome());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ProdutoIndisponivelException(e);
        }
    }

    public boolean existe(Long produtoId) {
        return buscarNome(produtoId).isPresent();
    }
}
