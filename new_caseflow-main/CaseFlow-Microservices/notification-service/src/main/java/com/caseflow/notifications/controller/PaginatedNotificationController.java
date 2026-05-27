package com.caseflow.notifications.controller;

import com.caseflow.notifications.entity.Notification;
import com.caseflow.notifications.service.CachedNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/paginated")
@RequiredArgsConstructor
@Tag(name = "Notifications — Paginated", description = "Paginated notification endpoints")
public class PaginatedNotificationController {

    private final CachedNotificationService cachedNotificationService;

    @GetMapping
    @Operation(summary = "Get all notifications (paginated)")
    public ResponseEntity<Page<Notification>> getAllNotificationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "notificationId,desc") String sort) {
        String[] s = sort.split(",");
        return ResponseEntity.ok(cachedNotificationService.getAllNotificationsPaginated(
            PageRequest.of(page, size, Sort.by(s.length > 1 && s[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, s[0]))));
    }
}
