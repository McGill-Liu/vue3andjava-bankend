package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.CustomerUserService;
import jakarta.validation.Valid;
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

    public AdminUserController(CustomerUserService customerUserService, AdminPermissionService adminPermissionService) {
        this.customerUserService = customerUserService;
        this.adminPermissionService = adminPermissionService;
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
    public ApiResponse<?> create(@Valid @RequestBody AdminDtos.CustomerCreateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok("客户已新增", customerUserService.create(request, SecurityUtils.currentUser()));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.CustomerUpdateRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok("客户已更新", customerUserService.update(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        customerUserService.resetPassword(id);
        return ApiResponse.ok("密码已重置为当前身份证号后 6 位", null);
    }

    @PutMapping("/{id}/points-balance")
    public ApiResponse<?> setBalance(@PathVariable Long id, @Valid @RequestBody AdminDtos.CustomerBalanceRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.USERS);
        return ApiResponse.ok("积分余额已调整", customerUserService.setBalance(id, request, SecurityUtils.currentUser()));
    }
}
