package com.app.med_support.controller;

import com.app.med_support.model.DonationReminder;
import com.app.med_support.model.User;
import com.app.med_support.service.DonationReminderService;
import com.app.med_support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/donation-reminders")
@Tag(name = "Donation Reminders",
        description = "APIs for scheduling blood donation appointment reminders. " +
                "Only authenticated donors can create reminders.")
public class DonationReminderController {

    private final DonationReminderService reminderService;
    private final UserService userService;


    public DonationReminderController(DonationReminderService reminderService,
            UserService userService) {

        this.reminderService = reminderService;
        this.userService = userService;
    }

    @Operation(summary = "Create donation reminder",
            description = "Allows an authenticated DONOR to schedule a reminder " +
                    "for their own donation booking. " +
                    "The reminder time must be in the future and before " + "the donation appointment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Donation reminder created successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. DONOR role is required",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Authenticated user not found",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error. Some invalid reminder requests currently also return 500",
                    content = @Content)
    })
    // Only authenticated donors can create reminders
    @PreAuthorize("hasRole('DONOR')")
    @PostMapping
    public ResponseEntity<DonationReminder> createReminder(
            @Parameter(description = "ID of the donation booking that belongs to the authenticated donor",
                    example = "18", required = true)
            @RequestParam Long bookingId,
            @Parameter(description = "Reminder date and time in ISO format. " +
                            "Must be in the future and before the donation appointment. " +
                            "Use the server's local date and time.",
                    example = "2026-11-01T09:00:00", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime reminderTime,
            Authentication authentication) {


        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        DonationReminder reminder = reminderService.createReminder(bookingId,
                reminderTime, user.getId());

        return ResponseEntity.ok(reminder);
    }
}
