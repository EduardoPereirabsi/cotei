package br.cotei.msalerta.messaging;

import br.cotei.msalerta.domain.Notificacao;
import br.cotei.msalerta.service.AlertaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consome oferta.atualizada e confere os alertas de preco.
 * O envio do aviso e simulado no log; a notificacao fica gravada para consulta.
 */
@Component
public class OfertaAtualizadaListener {

    private static final Logger log = LoggerFactory.getLogger(OfertaAtualizadaListener.class);

    private final AlertaService service;
    private final JsonMapper json;

    public OfertaAtualizadaListener(AlertaService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @RabbitListener(queues = RabbitConfig.FILA_ALERTA)
    public void receber(Message message) {
        OfertaAtualizadaEvent evento;
        try {
            evento = json.readValue(message.getBody(), OfertaAtualizadaEvent.class);
        } catch (JacksonException e) {
            log.warn("[ALERTA] mensagem descartada: JSON invalido");
            return;
        }
        if (evento.produtoId() == null || evento.preco() == null) {
            log.warn("[ALERTA] mensagem descartada: produtoId ou preco ausente");
            return;
        }

        if (!evento.disponivel()) {
            log.info("[ALERTA] oferta recebida: produto={} loja={} - sem estoque, nao dispara alertas",
                    evento.produtoId(), evento.loja());
            return;
        }

        List<Notificacao> geradas = service.processarOferta(evento.produtoId(), evento.loja(), evento.preco(),
                evento.frete(), evento.disponivel(), evento.url());

        BigDecimal total = evento.preco().add(evento.frete() == null ? BigDecimal.ZERO : evento.frete());
        if (geradas.isEmpty()) {
            log.info("[ALERTA] oferta recebida: produto={} loja={} total={} - nenhum alvo atingido",
                    evento.produtoId(), evento.loja(), total);
        }
        for (Notificacao n : geradas) {
            log.info("[ALERTA] DISPARADO -> {}: {}", n.getEmail(), n.getMensagem());
        }
    }
}
