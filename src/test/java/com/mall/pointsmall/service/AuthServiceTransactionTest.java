package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(AuthService.class)
class AuthServiceTransactionTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AdminPermissionService adminPermissionService;

    @MockitoBean
    private CustomerSessionService customerSessionService;

    @MockitoBean
    private AdminSessionService adminSessionService;

    @MockitoBean
    private WeChatMiniProgramService weChatMiniProgramService;

    @Test
    void failedAttemptsAreCommittedEvenWhenLoginThrows() {
        CustomerUser customer = new CustomerUser();
        customer.setName("事务测试客户");
        customer.setPhone("13900000001");
        customer.setIdCardNo("transaction-test-id-card");
        customer.setPasswordHash("encoded-password");
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customerUserRepository.saveAndFlush(customer);

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest();
        request.setPhone(customer.getPhone());
        request.setPassword("wrong-password");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(BusinessException.class, () -> authService.loginUser(request));
        }

        entityManager.flush();
        entityManager.clear();
        CustomerUser persisted = customerUserRepository.findByPhone(customer.getPhone()).orElseThrow();
        assertEquals(5, persisted.getFailedLoginAttempts());
        assertTrue(persisted.getLoginLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
    }
}
