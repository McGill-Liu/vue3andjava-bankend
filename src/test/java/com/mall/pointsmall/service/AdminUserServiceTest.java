package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {
    @Test
    void managementActionsCannotResetOrDisableSuperAdmin() {
        AdminUserRepository repository = mock(AdminUserRepository.class);
        AdminUser superAdmin = new AdminUser();
        superAdmin.setId(1L);
        superAdmin.setRole(RoleType.SUPER_ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(superAdmin));

        AdminUserService service = new AdminUserService(
                repository,
                mock(PasswordEncoder.class),
                mock(AdminPermissionService.class),
                mock(AdminSessionService.class)
        );

        assertThrows(BusinessException.class, () -> service.resetPassword(1L, "NewPass123"));
        assertThrows(BusinessException.class, () -> service.updateStatus(1L, false));
        assertThrows(BusinessException.class, () -> service.update(1L, new AdminDtos.AdminUpdateRequest()));
    }

    @Test
    void managementCanResetAnOperatorPassword() {
        AdminUserRepository repository = mock(AdminUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminPermissionService permissionService = mock(AdminPermissionService.class);
        AdminSessionService sessionService = mock(AdminSessionService.class);
        AdminUser operator = new AdminUser();
        operator.setId(2L);
        operator.setName("业务员");
        operator.setEmail("operator@example.com");
        operator.setRole(RoleType.OPERATOR);
        operator.setEnabled(true);
        when(repository.findById(2L)).thenReturn(Optional.of(operator));
        when(repository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-new-password");
        when(permissionService.resolvedPermissions(operator)).thenReturn(Map.of());

        AdminUserService service = new AdminUserService(repository, passwordEncoder, permissionService, sessionService);

        service.resetPassword(2L, "NewPass123");

        assertEquals("encoded-new-password", operator.getPasswordHash());
        verify(sessionService).revoke(2L);
    }
}
