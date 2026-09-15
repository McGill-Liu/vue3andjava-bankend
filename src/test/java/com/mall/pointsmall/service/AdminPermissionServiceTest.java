package com.mall.pointsmall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.security.SecurityUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminPermissionServiceTest {
    private final AdminPermissionService service = new AdminPermissionService(new ObjectMapper());

    @Test
    void viewPermissionAllowsReadingButRejectsEditing() {
        SecurityUser operator = operator(Map.of(AdminMenuKey.USERS.name(), "VIEW"));

        assertDoesNotThrow(() -> service.assertView(operator, AdminMenuKey.USERS));
        assertThrows(BusinessException.class, () -> service.assertEdit(operator, AdminMenuKey.USERS));
    }

    @Test
    void missingOrInvalidPermissionFailsClosedAsBusinessError() {
        assertThrows(BusinessException.class, () -> service.assertView(operator(Map.of()), AdminMenuKey.ORDERS));
        assertThrows(BusinessException.class,
                () -> service.assertView(operator(Map.of(AdminMenuKey.ORDERS.name(), "UNKNOWN")), AdminMenuKey.ORDERS));
    }

    @Test
    void superAdminCannotBeRestrictedByEditablePermissionData() {
        SecurityUser superAdmin = new SecurityUser(1L, "超级管理员", null, RoleType.SUPER_ADMIN, Map.of(), false);
        assertDoesNotThrow(() -> service.assertEdit(superAdmin, AdminMenuKey.ADMINS));
    }

    private SecurityUser operator(Map<String, String> permissions) {
        return new SecurityUser(2L, "业务员", null, RoleType.OPERATOR, permissions, false);
    }
}
