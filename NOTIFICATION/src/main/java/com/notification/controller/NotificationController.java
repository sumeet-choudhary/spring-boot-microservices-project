package com.notification.controller;

import com.notification.exception.BadRequestException;
import com.notification.model.entity.Notification;
import com.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<Notification> sendManual(@RequestBody Map<String, Object> body) {
        Object empId = body.get("empId");
        String recipient = (String) body.get("recipient");
        String message = (String) body.get("message");
        if (empId == null || recipient == null || message == null) {
            throw new BadRequestException("empId, recipient and message are required", HttpStatus.BAD_REQUEST);
        }
        Notification response = notificationService.sendManual(Long.valueOf(empId.toString()), recipient, message);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> response = notificationService.getAllNotifications();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/empId/{empId}")
    public ResponseEntity<List<Notification>> getNotificationsByEmpId(@PathVariable Long empId) {
        List<Notification> response = notificationService.getNotificationsByEmpId(empId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
