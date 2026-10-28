package br.cotei.msloja.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fila propria no exchange fanout: as replicas do ms-loja disputam esta fila,
 * mas o ms-alerta recebe os mesmos eventos pela dele.
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
