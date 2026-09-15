package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.AdminUserService;
import com.mall.pointsmall.service.OperationRecordService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/admin-users")
public class AdminManageController {
    private final AdminUserService adminUserService;
    private final AdminPermissionService adminPermissionService;
    private final OperationRecordService operationRecordService;

    public AdminManageController(AdminUserService adminUserService, AdminPermissionService adminPermissionService,
                                 OperationRecordService operationRecordService) {
        this.adminUserService = adminUserService;
        this.adminPermissionService = adminPermissionService;
        this.operationRecordService = operationRecordService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        return ApiResponse.ok(adminUserService.list());
    }

    @PostMapping
    @Transactional
    public ApiResponse<?> create(@Valid @RequestBody AdminDtos.AdminCreateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        AdminDtos.AdminResponse created = adminUserService.create(request);
        operationRecordService.recordSuccess("新增管理员", "管理员", created.getId(), created.getName(), "管理员账号已创建");
        return ApiResponse.ok(created);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminUpdateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        AdminDtos.AdminResponse updated = adminUserService.update(id, request);
        operationRecordService.recordSuccess("修改管理员权限", "管理员", updated.getId(), updated.getName(), "管理员资料或权限已更新");
        return ApiResponse.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminStatusRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        AdminDtos.AdminResponse updated = adminUserService.updateStatus(id, request.getEnabled());
        operationRecordService.recordSuccess("修改管理员状态", "管理员", updated.getId(), updated.getName(),
                updated.isEnabled() ? "管理员已启用" : "管理员已停用");
        return ApiResponse.ok("状态已更新", null);
    }

    @PatchMapping("/{id}/reset-password")
    @Transactional
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminDtos.AdminResetPasswordRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ADMINS);
        AdminDtos.AdminResponse updated = adminUserService.resetPassword(id, request.getPassword());
        operationRecordService.recordSuccess("重置管理员密码", "管理员", updated.getId(), updated.getName(),
                "管理员密码已重置（密码内容不记录）");
        return ApiResponse.ok("密码已重置", null);
    }
}
