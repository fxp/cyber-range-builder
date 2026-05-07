package com.cyberrange.pointsmall.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PointsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PointsEventConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.points-earned:points.earned}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePointsEarned(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            log.info("Points earned event received - userId: {}, points: {}, source: {}",
                    event.get("userId"), event.get("points"), event.get("reference"));
        } catch (Exception e) {
            log.error("Failed to process points earned event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.points-spent:points.spent}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePointsSpent(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            log.info("Points spent event received - userId: {}, points: {}, orderNo: {}",
                    event.get("userId"), event.get("points"), event.get("reference"));
        } catch (Exception e) {
            log.error("Failed to process points spent event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.order-created:order.created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            log.info("Order created event received - orderNo: {}, userId: {}, totalPoints: {}",
                    event.get("orderNo"), event.get("userId"), event.get("totalPoints"));
        } catch (Exception e) {
            log.error("Failed to process order created event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.order-completed:order.completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCompleted(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            log.info("Order completed event received - orderNo: {}, userId: {}",
                    event.get("orderNo"), event.get("userId"));
        } catch (Exception e) {
            log.error("Failed to process order completed event: {}", e.getMessage());
        }
    }
}
