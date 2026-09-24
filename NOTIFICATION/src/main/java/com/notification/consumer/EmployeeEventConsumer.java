package com.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.EmployeeEvent;
import com.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmployeeEventConsumer.class);

    private final NotificationService notificationService;
    private final KafkaTemplate<String, Object> dlqKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.employee-events-dlq}")
    private String dlqTopic;

    public EmployeeEventConsumer(NotificationService notificationService,
                                 KafkaTemplate<String, Object> dlqKafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.employee-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            EmployeeEvent event = objectMapper.readValue(record.value(), EmployeeEvent.class);
            if (event.getEventId() == null || event.getEmpId() == null) {
                throw new IllegalArgumentException("Invalid employee event, missing eventId or empId");
            }
            notificationService.handleEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process record from {}, sending to DLQ: {}", record.topic(), e.getMessage());
            try {
                dlqKafkaTemplate.send(dlqTopic, record.key(), record.value());
            } catch (Exception dlqEx) {
                log.error("Failed to send record to DLQ: {}", dlqEx.getMessage());
            }
            acknowledgment.acknowledge();
        }
    }
}
