package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminPermissionService adminPermissionService;
    private final AdminSessionService adminSessionService;

    public AdminUserService(AdminUserRepository adminUserRepository,
                            PasswordEncoder passwordEncoder,
                            AdminPermissionService adminPermissionService,
                            AdminSessionService adminSessionService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPermissionService = adminPermissionService;
        this.adminSessionService = adminSessionService;
    }

    public List<AdminDtos.AdminResponse> list() {
        return adminUserRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminDtos.AdminResponse create(AdminDtos.AdminCreateRequest request) {
        if (adminUserRepository.existsByName(request.getName())) {
            throw new BusinessException("管理员名称已存在");
        }
        AdminUser adminUser = new AdminUser();
        adminUser.setName(request.getName());
        adminUser.setEmail(request.getEmail());
        adminUser.setRole(RoleType.OPERATOR);
        adminUser.setEnabled(true);
        adminUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        Map<String, String> permissions = adminPermissionService.normalizeOperatorPermissions(request.getPermissions());
        adminUser.setPermissionsJson(adminPermissionService.toJson(permissions));
        AdminDtos.AdminResponse response = toResponse(adminUserRepository.save(adminUser));
        adminSessionService.revoke(adminUser.getId());
        return response;
    }

    @Transactional
    public AdminDtos.AdminResponse update(Long id, AdminDtos.AdminUpdateRequest request) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (adminUser.getRole() == RoleType.SUPER_ADMIN) {
            throw new BusinessException("不能修改超级管理员权限");
        }
        if (!adminUser.getName().equals(request.getName()) && adminUserRepository.existsByName(request.getName())) {
            throw new BusinessException("管理员名称已存在");
        }
        adminUser.setName(request.getName());
        adminUser.setEmail(request.getEmail());
        adminUser.setPermissionsJson(adminPermissionService.toJson(
                adminPermissionService.normalizeOperatorPermissions(request.getPermissions())
        ));
        AdminDtos.AdminResponse response = toResponse(adminUserRepository.save(adminUser));
        adminSessionService.revoke(id);
        return response;
    }

    @Transactional
    public AdminDtos.AdminResponse updateStatus(Long id, boolean enabled) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> new BusinessException("管理员不存在"));
        assertNotSuperAdmin(adminUser, "不能停用或启用超级管理员");
        adminUser.setEnabled(enabled);
        AdminDtos.AdminResponse response = toResponse(adminUserRepository.save(adminUser));
        adminSessionService.revoke(id);
        return response;
    }

    @Transactional
    public AdminDtos.AdminResponse resetPassword(Long id, String password) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> new BusinessException("管理员不存在"));
        assertNotSuperAdmin(adminUser, "超级管理员只能登录后修改本人密码");
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        AdminDtos.AdminResponse response = toResponse(adminUserRepository.save(adminUser));
        adminSessionService.revoke(id);
        return response;
    }

    private AdminDtos.AdminResponse toResponse(AdminUser adminUser) {
        AdminDtos.AdminResponse response = new AdminDtos.AdminResponse();
        response.setId(adminUser.getId());
        response.setName(adminUser.getName());
        response.setEmail(adminUser.getEmail());
        response.setRole(adminUser.getRole().name());
        response.setEnabled(adminUser.isEnabled());
        response.setPermissions(adminPermissionService.resolvedPermissions(adminUser));
        return response;
    }

    private void assertNotSuperAdmin(AdminUser adminUser, String message) {
        if (adminUser.getRole() == RoleType.SUPER_ADMIN) {
            throw new BusinessException(message);
        }
    }
}
