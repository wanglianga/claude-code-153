package com.waste.mq;

import com.waste.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 业务事件总线。
 * 正常情况：事务提交后投递到 RabbitMQ，由对应队列监听器异步处理；
 * MQ 未启用或发送失败：降级为同步调用同一套处理器，保证业务不中断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventBus {

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final WasteEventListener listener;

    @Value("${app.mq.enabled:true}")
    private boolean mqEnabled;

    /** 在当前事务提交后发布事件（无事务则立即发布） */
    public void publish(String routingKey, Long entityId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(routingKey, entityId);
                }
            });
        } else {
            dispatch(routingKey, entityId);
        }
    }

    private void dispatch(String routingKey, Long entityId) {
        if (mqEnabled) {
            try {
                RabbitTemplate template = rabbitTemplateProvider.getIfAvailable();
                if (template != null) {
                    template.convertAndSend(RabbitConfig.EXCHANGE, routingKey, String.valueOf(entityId));
                    log.info("事件已投递 MQ: {} -> {}", routingKey, entityId);
                    return;
                }
            } catch (Exception e) {
                log.warn("RabbitMQ 投递失败，降级同步处理: {} {}", routingKey, e.getMessage());
            }
        }
        log.info("MQ 未启用，同步处理事件: {} -> {}", routingKey, entityId);
        listener.dispatch(routingKey, entityId);
    }
}
