package com.app.med_support.controller;

import com.app.med_support.model.Notification;
import com.app.med_support.service.BookingNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final BookingNotificationService notificationService;

    public NotificationController(BookingNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // notifications for the login user
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyNotifications(
            Authentication authentication) {
        List<Notification> notifications = notificationService.getMyNotifications(authentication.getName());
        List<Map<String, Object>> response = notifications.stream()
                .map(notification -> Map.<String, Object>of(
                        "id", notification.getId(),
                        "donationBookingId",
                        notification.getDonationBooking().getId(),
                        "notificationMessage", notification.getMessage(),
                        "notificationTime", notification.getCreatedAt(),
                        "isRead", notification.isRead()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
