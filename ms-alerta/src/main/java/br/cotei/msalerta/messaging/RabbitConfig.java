package br.cotei.msalerta.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_OFERTAS = "cotei.ofertas";
    public static final String FILA_ALERTA = "alerta.oferta-atualizada";

    @Bean
    FanoutExchange ofertasExchange() {
        return new FanoutExchange(EXCHANGE_OFERTAS, true, false);
    }

    @Bean
    Queue filaAlerta() {
        return new Queue(FILA_ALERTA, true);
    }

    @Bean
    Binding filaAlertaNoExchange(Queue filaAlerta, FanoutExchange ofertasExchange) {
        return BindingBuilder.bind(filaAlerta).to(ofertasExchange);
    }
}
