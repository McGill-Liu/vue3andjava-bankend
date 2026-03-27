package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AdminPermissionService adminPermissionService;

    public NotificationController(NotificationService notificationService, AdminPermissionService adminPermissionService) {
        this.notificationService = notificationService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping
    public ApiResponse<?> notifications() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.NOTIFICATIONS);
        return ApiResponse.ok(notificationService.list());
    }

    @GetMapping("/unprocessed-count")
    public ApiResponse<Long> unprocessedCount() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.NOTIFICATIONS);
        return ApiResponse.ok(notificationService.unprocessedCount());
    }

    @PostMapping("/{id}/process")
    public ApiResponse<Void> process(@PathVariable Long id) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.NOTIFICATIONS);
        notificationService.process(id, SecurityUtils.currentUser().getId());
        return ApiResponse.ok("通知已处理", null);
    }
}
