package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.entity.NotificationMessage;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.NotificationService;
import com.mall.pointsmall.service.OperationRecordService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AdminPermissionService adminPermissionService;
    private final OperationRecordService operationRecordService;

    public NotificationController(NotificationService notificationService, AdminPermissionService adminPermissionService,
                                  OperationRecordService operationRecordService) {
        this.notificationService = notificationService;
        this.adminPermissionService = adminPermissionService;
        this.operationRecordService = operationRecordService;
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
    @Transactional
    public ApiResponse<Void> process(@PathVariable Long id) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.NOTIFICATIONS);
        NotificationMessage message = notificationService.process(id, SecurityUtils.currentUser().getId());
        operationRecordService.recordSuccess("处理待办事项", "待办事项", message.getId(), message.getTitle(), "待办事项已标记为已处理");
        return ApiResponse.ok("待办事项已处理", null);
    }
}
