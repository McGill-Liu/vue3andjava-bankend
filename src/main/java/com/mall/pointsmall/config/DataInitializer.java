package com.mall.pointsmall.config;

import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.ProductCategoryRepository;
import com.mall.pointsmall.repository.ProductRepository;
import com.mall.pointsmall.service.AdminPermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final AdminUserRepository adminUserRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminPermissionService adminPermissionService;

    public DataInitializer(AdminUserRepository adminUserRepository,
                           ProductCategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           AdminPermissionService adminPermissionService) {
        this.adminUserRepository = adminUserRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPermissionService = adminPermissionService;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() == 0) {
            AdminUser superAdmin = new AdminUser();
            superAdmin.setName("超级管理员");
            superAdmin.setPhone("13800000000");
            superAdmin.setEmail("boss@example.com");
            superAdmin.setRole(RoleType.SUPER_ADMIN);
            superAdmin.setPermissionsJson(adminPermissionService.toJson(adminPermissionService.defaultPermissions(RoleType.SUPER_ADMIN)));
            superAdmin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            adminUserRepository.save(superAdmin);

            AdminUser operator = new AdminUser();
            operator.setName("业务员");
            operator.setPhone("13900000000");
            operator.setEmail("operator@example.com");
            operator.setRole(RoleType.OPERATOR);
            operator.setPermissionsJson(adminPermissionService.toJson(adminPermissionService.defaultPermissions(RoleType.OPERATOR)));
            operator.setPasswordHash(passwordEncoder.encode("Operator@123"));
            adminUserRepository.save(operator);
        } else {
            adminUserRepository.findAll().forEach(adminUser -> {
                if (adminUser.getPermissionsJson() == null || adminUser.getPermissionsJson().isBlank()) {
                    adminUser.setPermissionsJson(adminPermissionService.toJson(adminPermissionService.defaultPermissions(adminUser.getRole())));
                    adminUserRepository.save(adminUser);
                }
            });
        }
        if (categoryRepository.count() == 0) {
            ProductCategory category = new ProductCategory();
            category.setName("精选好物");
            category.setSortOrder(1);
            ProductCategory savedCategory = categoryRepository.save(category);

            Product product = new Product();
            product.setCategoryId(savedCategory.getId());
            product.setName("积分礼盒");
            product.setCoverImageUrl("https://dummyimage.com/320x320/f2f4f8/334155&text=Gift");
            product.setGalleryJson("[]");
            product.setPointsCost(200);
            product.setStock(50);
            product.setDescription("内部客户兑换使用的示例商品。");
            productRepository.save(product);
        }
    }
}
