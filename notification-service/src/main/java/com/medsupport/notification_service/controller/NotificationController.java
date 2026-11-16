
package com.medsupport.notification_service.controller;

import com.medsupport.notification_service.model.Notification;
import com.medsupport.notification_service.request.NotificationRequest;
import com.medsupport.notification_service.response.NotificationResponse;
import com.medsupport.notification_service.service.NotificationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.medsupport.notification_service.config.NotificationWebSocketConfig userNotifications;

    public record InboxItem(Long id, String notificationType, String title, String message,
            Long bookingId, java.time.LocalDateTime createdAt) {}

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    @GetMapping("/my")
    public org.springframework.data.domain.Page<InboxItem> inbox(
            @RequestHeader(value="Authorization", required=false) String bearer, @RequestParam(defaultValue="0") int page) {
        if (page < 0) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        String email = userNotifications.verifyEmail(bearer);
        return userNotifications.inbox(email, page);
    }

    public NotificationController(NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(
            @Valid @RequestBody NotificationRequest request) {

        Notification notification = notificationService.createNotification(
                request.getRecipientEmail(), request.getNotificationType(),
                request.getTitle(), request.getMessage(),
                request.getBookingId());

        return NotificationResponse.from(notification);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
    @PostMapping("/internal/{id}/send")
    public NotificationResponse sendNotification(@PathVariable Long id) {

        Notification notification = notificationService.sendNotification(id);

        return NotificationResponse.from(notification);
    }


    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
    @GetMapping("/internal/recipient")
    public List<NotificationResponse> getByRecipient(@RequestParam String email) {

        return notificationService
                .getNotificationsByRecipient(email)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
    @GetMapping("/internal/booking/{bookingId}")
    public List<NotificationResponse> getByBooking(@PathVariable Long bookingId) {

        return notificationService
                .getNotificationsByBooking(bookingId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
    @GetMapping("/internal/pending")
    public List<NotificationResponse> getPendingNotifications() {

        return notificationService
                .getPendingNotifications()
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
