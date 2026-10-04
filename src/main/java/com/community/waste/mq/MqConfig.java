package com.community.waste.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：topic 交换器 waste.events，按事件类型路由。
 * 队列：
 *  q.appointment  预约/派单事件（waste.appointment.#, waste.dispatch.#, waste.trip.late）
 *  q.violation    违规事件（waste.violation.#）
 *  q.weigh        称重异常（waste.weigh.#）
 *  q.complaint    投诉事件（waste.complaint.#）
 */
@Configuration
public class MqConfig {

    public static final String EXCHANGE = "waste.events";
    public static final String Q_APPOINTMENT = "q.appointment";
    public static final String Q_VIOLATION = "q.violation";
    public static final String Q_WEIGH = "q.weigh";
    public static final String Q_COMPLAINT = "q.complaint";

    public static final String RK_APPOINTMENT = "waste.appointment.#";
    public static final String RK_DISPATCH = "waste.dispatch.#";
    public static final String RK_TRIP_LATE = "waste.trip.late";
    public static final String RK_VIOLATION = "waste.violation.#";
    public static final String RK_WEIGH = "waste.weigh.anomaly";
    public static final String RK_COMPLAINT = "waste.complaint.#";

    @Bean
    TopicExchange wasteExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue appointmentQueue() {
        return QueueBuilder.durable(Q_APPOINTMENT).build();
    }

    @Bean
    Queue violationQueue() {
        return QueueBuilder.durable(Q_VIOLATION).build();
    }

    @Bean
    Queue weighQueue() {
        return QueueBuilder.durable(Q_WEIGH).build();
    }

    @Bean
    Queue complaintQueue() {
        return QueueBuilder.durable(Q_COMPLAINT).build();
    }

    @Bean
    Declarables bindings() {
        return new Declarables(
                BindingBuilder.bind(appointmentQueue()).to(wasteExchange()).with(RK_APPOINTMENT),
                BindingBuilder.bind(appointmentQueue()).to(wasteExchange()).with(RK_DISPATCH),
                BindingBuilder.bind(appointmentQueue()).to(wasteExchange()).with(RK_TRIP_LATE),
                BindingBuilder.bind(violationQueue()).to(wasteExchange()).with(RK_VIOLATION),
                BindingBuilder.bind(weighQueue()).to(wasteExchange()).with(RK_WEIGH),
                BindingBuilder.bind(complaintQueue()).to(wasteExchange()).with(RK_COMPLAINT));
    }

    @Bean
    MessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
