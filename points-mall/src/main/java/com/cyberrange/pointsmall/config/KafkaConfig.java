package com.cyberrange.pointsmall.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.points-earned:points.earned}")
    private String pointsEarnedTopic;

    @Value("${kafka.topics.points-spent:points.spent}")
    private String pointsSpentTopic;

    @Value("${kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-completed:order.completed}")
    private String orderCompletedTopic;

    @Bean
    public NewTopic pointsEarnedTopic() {
        return TopicBuilder.name(pointsEarnedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pointsSpentTopic() {
        return TopicBuilder.name(pointsSpentTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name(orderCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter() {
        return new JsonMessageConverter();
    }
}
