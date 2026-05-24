package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/admin-users")
public class AdminManageController {
    private final AdminUserService adminUserService;
    private final AdminPermissionService adminPermissionService;

    public AdminManageController(AdminUserService adminUserService, AdminPermissionService adminPermissionService) {
        this.adminUserService = adminUserService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        return ApiResponse.ok(adminUserService.list());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody AdminDtos.AdminCreateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        return ApiResponse.ok(adminUserService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminUpdateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        return ApiResponse.ok(adminUserService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminStatusRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        adminUserService.updateStatus(id, request.getEnabled());
        return ApiResponse.ok("状态已更新", null);
    }

    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminResetPasswordRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        adminUserService.resetPassword(id, request.getPassword());
        return ApiResponse.ok("密码已重置", null);
    }
}
