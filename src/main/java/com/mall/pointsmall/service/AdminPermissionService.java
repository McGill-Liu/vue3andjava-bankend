package com.mall.pointsmall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.enums.MenuPermissionLevel;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.security.SecurityUser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminPermissionService {
    private final ObjectMapper objectMapper;

    public AdminPermissionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> parsePermissions(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new BusinessException("管理员权限数据损坏");
        }
    }

    public String toJson(Map<String, String> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions == null ? new HashMap<>() : permissions);
        } catch (Exception ex) {
            throw new BusinessException("管理员权限保存失败");
        }
    }

    public Map<String, String> defaultPermissions(RoleType role) {
        Map<String, String> permissions = new LinkedHashMap<>();
        if (role == RoleType.SUPER_ADMIN) {
            permissions.put(AdminMenuKey.NOTIFICATIONS.name(), MenuPermissionLevel.EDIT.name());
            permissions.put(AdminMenuKey.USERS.name(), MenuPermissionLevel.EDIT.name());
            permissions.put(AdminMenuKey.POINTS.name(), MenuPermissionLevel.EDIT.name());
            permissions.put(AdminMenuKey.PRODUCTS.name(), MenuPermissionLevel.EDIT.name());
            permissions.put(AdminMenuKey.PRODUCT_TRANSACTIONS.name(), MenuPermissionLevel.VIEW.name());
            permissions.put(AdminMenuKey.ORDERS.name(), MenuPermissionLevel.EDIT.name());
            permissions.put(AdminMenuKey.ADMINS.name(), MenuPermissionLevel.EDIT.name());
            return permissions;
        }
        permissions.put(AdminMenuKey.NOTIFICATIONS.name(), MenuPermissionLevel.EDIT.name());
        permissions.put(AdminMenuKey.USERS.name(), MenuPermissionLevel.VIEW.name());
        permissions.put(AdminMenuKey.POINTS.name(), MenuPermissionLevel.VIEW.name());
        permissions.put(AdminMenuKey.PRODUCTS.name(), MenuPermissionLevel.EDIT.name());
        permissions.put(AdminMenuKey.PRODUCT_TRANSACTIONS.name(), MenuPermissionLevel.VIEW.name());
        permissions.put(AdminMenuKey.ORDERS.name(), MenuPermissionLevel.EDIT.name());
        return permissions;
    }

    public Map<String, String> resolvedPermissions(AdminUser adminUser) {
        if (adminUser.getRole() == RoleType.SUPER_ADMIN) {
            return defaultPermissions(RoleType.SUPER_ADMIN);
        }
        Map<String, String> parsed = parsePermissions(adminUser.getPermissionsJson());
        if (parsed.isEmpty()) {
            parsed = defaultPermissions(adminUser.getRole());
        }
        return parsed;
    }

    public Map<String, String> normalizeOperatorPermissions(Map<String, String> incoming) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (AdminMenuKey key : AdminMenuKey.values()) {
            String raw = incoming == null ? null : incoming.get(key.name());
            MenuPermissionLevel level = raw == null ? MenuPermissionLevel.NONE : MenuPermissionLevel.valueOf(raw);
            if (level != MenuPermissionLevel.NONE) {
                normalized.put(key.name(), level.name());
            }
        }
        return normalized;
    }

    public void assertView(SecurityUser user, AdminMenuKey menuKey) {
        assertPermission(user, menuKey, MenuPermissionLevel.VIEW);
    }

    public void assertEdit(SecurityUser user, AdminMenuKey menuKey) {
        assertPermission(user, menuKey, MenuPermissionLevel.EDIT);
    }

    private void assertPermission(SecurityUser user, AdminMenuKey menuKey, MenuPermissionLevel required) {
        if (user.getRole() == RoleType.SUPER_ADMIN) {
            return;
        }
        String raw = user.getPermissions() == null ? null : user.getPermissions().get(menuKey.name());
        MenuPermissionLevel actual = raw == null ? MenuPermissionLevel.NONE : MenuPermissionLevel.valueOf(raw);
        boolean allowed = actual == MenuPermissionLevel.EDIT || (required == MenuPermissionLevel.VIEW && actual == MenuPermissionLevel.VIEW);
        if (!allowed) {
            throw new BusinessException("没有权限访问该菜单");
        }
    }
}
