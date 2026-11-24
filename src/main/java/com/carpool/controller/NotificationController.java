package com.carpool.controller;

import com.carpool.model.User;
import com.carpool.service.NotificationService;
import com.carpool.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepo;

    public NotificationController(NotificationService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // 🔔 Get all notifications
    @GetMapping("/my/{userId}")
    public Object myNotifications(@PathVariable Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return service.getUserNotifications(user);
    }

    // 🔴 Unread count (for bell badge)
    @GetMapping("/unread-count/{userId}")
    public long unreadCount(@PathVariable Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return service.getUnreadCount(user);
    }

    // ✅ Mark one as read
    @PutMapping("/read/{id}")
    public void markRead(@PathVariable Long id) {
        service.markAsRead(id);
    }

    // ✅ Mark all as read
    @PutMapping("/read-all/{userId}")
    public void markAllRead(@PathVariable Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        service.markAllAsRead(user);
    }
}
