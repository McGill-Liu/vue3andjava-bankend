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

    public AdminUserService(AdminUserRepository adminUserRepository,
                            PasswordEncoder passwordEncoder,
                            AdminPermissionService adminPermissionService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPermissionService = adminPermissionService;
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
        return toResponse(adminUserRepository.save(adminUser));
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
        return toResponse(adminUserRepository.save(adminUser));
    }

    @Transactional
    public void updateStatus(Long id, boolean enabled) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> new BusinessException("管理员不存在"));
        adminUser.setEnabled(enabled);
        adminUserRepository.save(adminUser);
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> new BusinessException("管理员不存在"));
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUserRepository.save(adminUser);
    }

    private AdminDtos.AdminResponse toResponse(AdminUser adminUser) {
        AdminDtos.AdminResponse response = new AdminDtos.AdminResponse();
        response.setId(adminUser.getId());
        response.setName(adminUser.getName());
        response.setEmail(adminUser.getEmail());
        response.setEnabled(adminUser.isEnabled());
        response.setPermissions(adminPermissionService.resolvedPermissions(adminUser));
        return response;
    }
}
