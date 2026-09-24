package com.notification.service;

import com.notification.model.dto.EmployeeEvent;
import com.notification.model.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification handleEvent(EmployeeEvent event);

    Notification sendManual(Long empId, String recipient, String message);

    List<Notification> getAllNotifications();

    List<Notification> getNotificationsByEmpId(Long empId);
}
