package br.cotei.msloja.messaging;

import br.cotei.msloja.client.ProdutoIndisponivelException;
import br.cotei.msloja.domain.Oferta;
import br.cotei.msloja.service.OfertaService;
import br.cotei.msloja.service.ProdutoInexistenteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consome oferta.atualizada e registra a oferta.
 * Antes de gravar, valida o produto no ms-produto por REST: um unico fluxo
 * demonstra a comunicacao assincrona (RabbitMQ) e a sincrona (REST).
 *
 * Mensagens que nao podem ser processadas sao descartadas com log
 * (spring.rabbitmq.listener.simple.default-requeue-rejected=false),
 * para nao entrarem em laco infinito de reentrega.
 */
@Component
public class OfertaAtualizadaListener {

    private static final Logger log = LoggerFactory.getLogger(OfertaAtualizadaListener.class);

    private final OfertaService service;
    private final JsonMapper json;

    public OfertaAtualizadaListener(OfertaService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @RabbitListener(queues = RabbitConfig.FILA_LOJA)
    public void receber(Message message) {
        OfertaAtualizadaEvent evento;
        try {
            evento = json.readValue(message.getBody(), OfertaAtualizadaEvent.class);
        } catch (JacksonException e) {
            log.warn("[OFERTA] descartada: JSON invalido");
            return;
        }

        try {
            Oferta oferta = service.registrar(evento.produtoId(), evento.loja(), evento.preco(),
                    evento.frete(), evento.disponivel(), evento.url(), evento.coletadoEm());
            log.info("[OFERTA] registrada: produto={} loja={} preco={} (oferta {})",
                    oferta.getProdutoId(), oferta.getLoja(), oferta.getPreco(), oferta.getId());
        } catch (ProdutoInexistenteException e) {
            log.warn("[OFERTA] descartada: {}", e.getMessage());
        } catch (ProdutoIndisponivelException e) {
            log.error("[OFERTA] descartada: ms-produto indisponivel para validar o produto {}", evento.produtoId());
        }
    }
}
