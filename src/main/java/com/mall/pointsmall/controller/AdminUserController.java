package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.CustomerUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final CustomerUserService customerUserService;
    private final AdminPermissionService adminPermissionService;

    public AdminUserController(CustomerUserService customerUserService, AdminPermissionService adminPermissionService) {
        this.customerUserService = customerUserService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/user-approvals")
    public ApiResponse<?> approvals() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.APPROVALS);
        return ApiResponse.ok(customerUserService.pendingApprovals());
    }

    @PostMapping("/user-approvals/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @Valid @RequestBody AdminDtos.ApproveUserRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.APPROVALS);
        customerUserService.approve(id, request.getInitialPoints(), SecurityUtils.currentUser());
        return ApiResponse.ok("审核通过", null);
    }

    @PostMapping("/user-approvals/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.APPROVALS);
        customerUserService.reject(id);
        return ApiResponse.ok("已驳回", null);
    }

    @GetMapping("/users")
    public ApiResponse<?> users() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.listAll());
    }

    @GetMapping("/users/{id}")
    public ApiResponse<?> user(@PathVariable Long id) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.getById(id));
    }

    @PatchMapping("/users/{id}/phone")
    public ApiResponse<?> updatePhone(@PathVariable Long id, @Valid @RequestBody AdminDtos.UpdatePhoneRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.updatePhone(id, request));
    }

    @PatchMapping("/users/{id}/id-card")
    public ApiResponse<?> updateIdCard(@PathVariable Long id, @Valid @RequestBody AdminDtos.UpdateIdCardRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.updateIdCard(id, request));
    }

    @PatchMapping("/users/{id}/password")
    public ApiResponse<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody AdminDtos.UpdatePasswordRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        customerUserService.updatePassword(id, request);
        return ApiResponse.ok("密码已更新", null);
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.UpdateStatusRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.updateStatus(id, request));
    }
}
