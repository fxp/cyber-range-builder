package com.cyberrange.pointsmall.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class PointsEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PointsEventProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.points-earned:points.earned}")
    private String pointsEarnedTopic;

    @Value("${kafka.topics.points-spent:points.spent}")
    private String pointsSpentTopic;

    @Value("${kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-completed:order.completed}")
    private String orderCompletedTopic;

    public void sendPointsEarnedEvent(Long userId, Long points, String source) {
        sendEvent(pointsEarnedTopic, userId.toString(), buildEvent("POINTS_EARNED", userId, points, source));
    }

    public void sendPointsSpentEvent(Long userId, Long points, String orderNo) {
        sendEvent(pointsSpentTopic, userId.toString(), buildEvent("POINTS_SPENT", userId, points, orderNo));
    }

    public void sendOrderCreatedEvent(Long userId, String orderNo, Long totalPoints) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_CREATED");
        payload.put("userId", userId);
        payload.put("orderNo", orderNo);
        payload.put("totalPoints", totalPoints);
        payload.put("timestamp", LocalDateTime.now().toString());
        sendEvent(orderCreatedTopic, orderNo, payload);
    }

    public void sendOrderCompletedEvent(Long userId, String orderNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_COMPLETED");
        payload.put("userId", userId);
        payload.put("orderNo", orderNo);
        payload.put("timestamp", LocalDateTime.now().toString());
        sendEvent(orderCompletedTopic, orderNo, payload);
    }

    private Map<String, Object> buildEvent(String type, Long userId, Long points, String ref) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", type);
        event.put("userId", userId);
        event.put("points", points);
        event.put("reference", ref);
        event.put("timestamp", LocalDateTime.now().toString());
        return event;
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
                } else {
                    log.debug("Event sent to topic {} partition {} offset {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error serializing event for topic {}: {}", topic, e.getMessage());
        }
    }
}
