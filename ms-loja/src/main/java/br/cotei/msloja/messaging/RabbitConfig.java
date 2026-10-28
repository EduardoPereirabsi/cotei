package br.cotei.msloja.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do evento oferta.atualizada (ver docs/CONVENCOES.md).
 *
 * Exchange fanout: cada servico interessado tem a PROPRIA fila e recebe TODOS os eventos.
 * Replicas do ms-loja disputam a fila loja.oferta-atualizada (competing consumers),
 * mas nao disputam com o ms-alerta, que tem a fila dele.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_OFERTAS = "cotei.ofertas";
    public static final String FILA_LOJA = "loja.oferta-atualizada";

    @Bean
    FanoutExchange ofertasExchange() {
        return new FanoutExchange(EXCHANGE_OFERTAS, true, false);
    }

    @Bean
    Queue filaLoja() {
        return new Queue(FILA_LOJA, true);
    }

    @Bean
    Binding filaLojaNoExchange(Queue filaLoja, FanoutExchange ofertasExchange) {
        return BindingBuilder.bind(filaLoja).to(ofertasExchange);
    }
}
