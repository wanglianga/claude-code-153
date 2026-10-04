package com.community.waste.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器。
 * RabbitMQ 可用时投递到 waste.events；不可用时降级为直接同步调用 EventProcessor，
 * 保证本地无 broker 的开发环境业务不中断（日志中会记录降级）。
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final EventProcessor fallbackProcessor;

    public EventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplate,
                          @Lazy EventProcessor fallbackProcessor) {
        this.rabbitTemplate = rabbitTemplate;
        this.fallbackProcessor = fallbackProcessor;
    }

    public void publish(String routingKey, DomainEvent event) {
        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            log.warn("RabbitTemplate 不可用，事件 {} 同步降级处理", routingKey);
            fallbackProcessor.handle(event);
            return;
        }
        try {
            template.convertAndSend(MqConfig.EXCHANGE, routingKey, event);
        } catch (AmqpException | IllegalStateException ex) {
            log.warn("RabbitMQ 投递失败（{}），事件 {} 同步降级处理：{}",
                    ex.getMessage(), routingKey, event.getEventType());
            fallbackProcessor.handle(event);
        }
    }
}
