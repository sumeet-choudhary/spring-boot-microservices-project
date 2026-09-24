package com.notification.service.impl;

import com.commomlib.exception.ResourceNotFoundException;
import com.notification.model.dto.EmployeeEvent;
import com.notification.model.entity.Notification;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String IDEMPOTENCY_PREFIX = "notification:processed:";
    private static final String EMP_SNAPSHOT_PREFIX = "employee:snapshot:";

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.notificationRepository = notificationRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Notification handleEvent(EmployeeEvent event) {
        String idempotencyKey = IDEMPOTENCY_PREFIX + event.getEventId();
        Boolean firstSeen = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(firstSeen)) {
            log.info("Duplicate event ignored: {}", event.getEventId());
            return notificationRepository.findByEventId(event.getEventId()).orElse(null);
        }

        if (notificationRepository.findByEventId(event.getEventId()).isPresent()) {
            return notificationRepository.findByEventId(event.getEventId()).get();
        }

        String message = buildMessage(event);
        simulateSend(event.getEmpEmail(), message);

        Notification notification = new Notification();
        notification.setEventId(event.getEventId());
        notification.setEventType(event.getEventType());
        notification.setEmpId(event.getEmpId());
        notification.setRecipient(event.getEmpEmail());
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        redisTemplate.opsForValue().set(EMP_SNAPSHOT_PREFIX + event.getEmpId(), event, Duration.ofHours(1));
        return saved;
    }

    @Override
    public Notification sendManual(Long empId, String recipient, String message) {
        simulateSend(recipient, message);
        Notification notification = new Notification();
        notification.setEventId("manual-" + System.currentTimeMillis());
        notification.setEventType("MANUAL");
        notification.setEmpId(empId);
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getAllNotifications() {
        List<Notification> all = notificationRepository.findAll();
        if (all.isEmpty()) {
            throw new ResourceNotFoundException("No notifications found");
        }
        return all;
    }

    @Override
    public List<Notification> getNotificationsByEmpId(Long empId) {
        List<Notification> list = notificationRepository.findAllByEmpId(empId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("No notifications found for employee id: " + empId);
        }
        return list;
    }

    private String buildMessage(EmployeeEvent event) {
        if ("EMPLOYEE_UPDATED".equals(event.getEventType())) {
            return "Hello " + event.getEmpName() + ", your employee profile was updated.";
        }
        return "Hello " + event.getEmpName() + ", welcome aboard! Your employee record was created.";
    }

    private void simulateSend(String recipient, String message) {
        log.info("Sending notification to {}: {}", recipient, message);
    }
}
