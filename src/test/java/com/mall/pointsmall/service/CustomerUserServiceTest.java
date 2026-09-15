package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.CustomerAddressRepository;
import com.mall.pointsmall.security.SecurityUser;
import com.mall.pointsmall.enums.RoleType;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerUserServiceTest {
    @Test
    void createsSixDigitTemporaryPasswordThatRequiresChange() {
        CustomerUserRepository repository = mock(CustomerUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PointsService pointsService = mock(PointsService.class);
        CustomerSessionService sessionService = mock(CustomerSessionService.class);
        CustomerAddressRepository addressRepository = mock(CustomerAddressRepository.class);
        CustomerUserService service = new CustomerUserService(repository, passwordEncoder, pointsService, sessionService, addressRepository);
        when(repository.findByPhone(anyString())).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(repository.save(any(CustomerUser.class))).thenAnswer(invocation -> {
            CustomerUser user = invocation.getArgument(0);
            user.setId(101L);
            return user;
        });

        AdminDtos.CustomerCreateRequest request = new AdminDtos.CustomerCreateRequest();
        request.setName("张三");
        request.setPhone("13800138000");
        request.setIdCardNo("110101199001011234");
        request.setInitialPoints(20);
        SecurityUser actor = new SecurityUser(1L, "超级管理员", null, RoleType.SUPER_ADMIN, Map.of(), false);

        AdminDtos.CustomerTemporaryPasswordResponse result = service.create(request, actor);

        assertEquals(101L, result.getCustomerId());
        assertTrue(result.getTemporaryPassword().matches("^\\d{6}$"));
        assertNotNull(result.getExpiresAt());
    }

    @Test
    void adjustingPointsDoesNotInvalidateCustomerSession() {
        CustomerUserRepository repository = mock(CustomerUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PointsService pointsService = mock(PointsService.class);
        CustomerSessionService sessionService = mock(CustomerSessionService.class);
        CustomerAddressRepository addressRepository = mock(CustomerAddressRepository.class);
        CustomerUserService service = new CustomerUserService(repository, passwordEncoder, pointsService, sessionService, addressRepository);
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        customer.setName("测试客户");
        customer.setPhone("13800138000");
        customer.setIdCardNo("110101199001011234");
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(pointsService.balanceOf(1L)).thenReturn(99);
        when(addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(1L)).thenReturn(List.of());

        AdminDtos.CustomerBalanceRequest request = new AdminDtos.CustomerBalanceRequest();
        request.setTargetBalance(99);
        request.setRemark("并发下单测试");
        SecurityUser actor = new SecurityUser(9L, "超级管理员", null, RoleType.SUPER_ADMIN, Map.of(), false);

        AdminDtos.CustomerResponse result = service.setBalance(1L, request, actor);

        assertEquals(99, result.getPointsBalance());
        verify(pointsService).setBalance(1L, 99, "并发下单测试", actor);
        verifyNoInteractions(sessionService);
    }
}
