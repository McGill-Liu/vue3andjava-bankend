package com.mall.pointsmall.config;

import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.service.AdminPermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.bootstrap.super-admin.enabled", havingValue = "true")
public class SuperAdminBootstrapInitializer implements CommandLineRunner {
    private final AdminUserRepository adminUserRepository;
    private final AdminPermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public SuperAdminBootstrapInitializer(AdminUserRepository adminUserRepository,
                                          AdminPermissionService permissionService,
                                          PasswordEncoder passwordEncoder,
                                          @Value("${app.bootstrap.super-admin.name}") String name,
                                          @Value("${app.bootstrap.super-admin.email}") String email,
                                          @Value("${app.bootstrap.super-admin.password}") String password) {
        this.adminUserRepository = adminUserRepository;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() > 0) {
            return;
        }
        if (name == null || name.isBlank() || email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException("初始化超级管理员名称或邮箱配置无效");
        }
        if (!password.matches("^[A-Za-z0-9-]{6,18}$")) {
            throw new IllegalStateException("初始化超级管理员密码为 6-18 位，只能包含数字、英文字母和连字符");
        }
        AdminUser admin = new AdminUser();
        admin.setName(name.trim());
        admin.setEmail(email.trim());
        admin.setRole(RoleType.SUPER_ADMIN);
        admin.setEnabled(true);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setPermissionsJson(permissionService.toJson(permissionService.defaultPermissions(RoleType.SUPER_ADMIN)));
        adminUserRepository.save(admin);
    }
}
