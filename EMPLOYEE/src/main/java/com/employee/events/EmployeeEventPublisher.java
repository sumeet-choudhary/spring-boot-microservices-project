package com.employee.events;

import com.employee.model.dto.EmployeeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmployeeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmployeeEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.employee-events:employee-events}")
    private String employeeEventsTopic;

    public EmployeeEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(EmployeeDto employee) {
        publish("EMPLOYEE_CREATED", employee);
    }

    public void publishUpdated(EmployeeDto employee) {
        publish("EMPLOYEE_UPDATED", employee);
    }

    private void publish(String eventType, EmployeeDto employee) {
        try {
            EmployeeEvent event = new EmployeeEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    employee.getId(),
                    employee.getEmpName(),
                    employee.getEmpEmail());
            kafkaTemplate.send(employeeEventsTopic, String.valueOf(employee.getId()), event);
            log.info("Published {} for employee id {}", eventType, employee.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} for employee id {}: {}",
                    eventType, employee.getId(), e.getMessage());
        }
    }
}
