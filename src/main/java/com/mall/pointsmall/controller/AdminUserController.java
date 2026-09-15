package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.CustomerUserService;
import com.mall.pointsmall.service.OperationRecordService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final CustomerUserService customerUserService;
    private final AdminPermissionService adminPermissionService;
    private final OperationRecordService operationRecordService;

    public AdminUserController(CustomerUserService customerUserService, AdminPermissionService adminPermissionService,
                               OperationRecordService operationRecordService) {
        this.customerUserService = customerUserService;
        this.adminPermissionService = adminPermissionService;
        this.operationRecordService = operationRecordService;
    }

    @GetMapping
    public ApiResponse<?> users() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> user(@PathVariable Long id) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok(customerUserService.getById(id));
    }

    @PostMapping
    @Transactional
    public ApiResponse<?> create(@Valid @RequestBody AdminDtos.CustomerCreateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        AdminDtos.CustomerTemporaryPasswordResponse created = customerUserService.create(request, SecurityUtils.currentUser());
        operationRecordService.recordSuccess("新增客户", "客户", created.getCustomerId(), created.getCustomerName(),
                "客户已创建并生成临时密码（密码内容不记录）");
        return ApiResponse.ok("客户已新增", created);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.CustomerUpdateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        AdminDtos.CustomerResponse updated = customerUserService.update(id, request);
        operationRecordService.recordSuccess("修改客户资料", "客户", updated.getId(), updated.getName(), "客户资料已更新");
        return ApiResponse.ok("客户已更新", updated);
    }

    @PostMapping("/{id}/reset-password")
    @Transactional
    public ApiResponse<?> resetPassword(@PathVariable Long id) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        AdminDtos.CustomerTemporaryPasswordResponse reset = customerUserService.resetPassword(id);
        operationRecordService.recordSuccess("重置客户密码", "客户", reset.getCustomerId(), reset.getCustomerName(),
                "已生成新的临时密码（密码内容不记录）");
        return ApiResponse.ok("临时密码已重置", reset);
    }

    @PutMapping("/{id}/points-balance")
    @Transactional
    public ApiResponse<?> setBalance(@PathVariable Long id, @Valid @RequestBody AdminDtos.CustomerBalanceRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        AdminDtos.CustomerResponse updated = customerUserService.setBalance(id, request, SecurityUtils.currentUser());
        operationRecordService.recordSuccess("调整客户积分", "客户", updated.getId(), updated.getName(),
                "积分余额调整为 " + updated.getPointsBalance());
        return ApiResponse.ok("积分余额已调整", updated);
    }
}
