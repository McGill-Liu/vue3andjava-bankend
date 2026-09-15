package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.PointsAccountRepository;
import com.mall.pointsmall.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void locksCustomerForFifteenMinutesAfterFiveWrongPasswords() {
        CustomerUserRepository customers = mock(CustomerUserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        CustomerUser customer = new CustomerUser();
        customer.setId(8L);
        customer.setPhone("13800138000");
        customer.setPasswordHash("encoded-password");
        when(customers.findByPhoneForUpdate(customer.getPhone())).thenReturn(Optional.of(customer));
        when(customers.save(any(CustomerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwords.matches(anyString(), anyString())).thenReturn(false);

        AuthService service = new AuthService(
                mock(AdminUserRepository.class), customers, passwords, mock(JwtTokenProvider.class),
                mock(AdminPermissionService.class), mock(PointsAccountRepository.class), mock(CustomerSessionService.class),
                mock(AdminSessionService.class), mock(WeChatMiniProgramService.class)
        );
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest();
        request.setPhone(customer.getPhone());
        request.setPassword("wrong-password");

        for (int index = 0; index < 5; index++) {
            assertThrows(BusinessException.class, () -> service.loginUser(request));
        }

        assertEquals(5, customer.getFailedLoginAttempts());
        assertTrue(customer.getLoginLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
    }
}
