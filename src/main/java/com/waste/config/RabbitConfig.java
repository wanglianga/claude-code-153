package com.waste.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 拓扑：一个 topic 交换机 + 四类业务队列 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "waste.exchange";

    public static final String Q_APPOINTMENT = "waste.appointment.queue";
    public static final String Q_WEIGH = "waste.weigh.queue";
    public static final String Q_VIOLATION = "waste.violation.queue";
    public static final String Q_COMPLAINT = "waste.complaint.queue";

    public static final String RK_APPOINTMENT_APPROVED = "appointment.approved";
    public static final String RK_WEIGH_RECORDED = "weigh.recorded";
    public static final String RK_VIOLATION_CREATED = "violation.created";
    public static final String RK_COMPLAINT_CREATED = "complaint.created";

    @Bean
    public TopicExchange wasteExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentQueue() {
        return QueueBuilder.durable(Q_APPOINTMENT).build();
    }

    @Bean
    public Queue weighQueue() {
        return QueueBuilder.durable(Q_WEIGH).build();
    }

    @Bean
    public Queue violationQueue() {
        return QueueBuilder.durable(Q_VIOLATION).build();
    }

    @Bean
    public Queue complaintQueue() {
        return QueueBuilder.durable(Q_COMPLAINT).build();
    }

    @Bean
    public Binding appointmentBinding() {
        return BindingBuilder.bind(appointmentQueue()).to(wasteExchange()).with(RK_APPOINTMENT_APPROVED);
    }

    @Bean
    public Binding weighBinding() {
        return BindingBuilder.bind(weighQueue()).to(wasteExchange()).with(RK_WEIGH_RECORDED);
    }

    @Bean
    public Binding violationBinding() {
        return BindingBuilder.bind(violationQueue()).to(wasteExchange()).with(RK_VIOLATION_CREATED);
    }

    @Bean
    public Binding complaintBinding() {
        return BindingBuilder.bind(complaintQueue()).to(wasteExchange()).with(RK_COMPLAINT_CREATED);
    }
}
