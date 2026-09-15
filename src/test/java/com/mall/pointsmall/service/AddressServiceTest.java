package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.repository.CustomerAddressRepository;
import com.mall.pointsmall.repository.CustomerUserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceTest {

    @Test
    void firstAddressIsAlwaysDefault() {
        Dependencies dependencies = dependencies();
        when(dependencies.addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(1L)).thenReturn(List.of());
        OrderDtos.AddressRequest request = request(false);

        CustomerAddress saved = dependencies.service.create(1L, request);

        assertTrue(saved.isDefaultAddress());
        verify(dependencies.addresses).save(saved);
    }

    @Test
    void currentDefaultCannotBeUnsetWithoutAnotherDefault() {
        Dependencies dependencies = dependencies();
        CustomerAddress current = address(10L, true);
        when(dependencies.addresses.findById(10L)).thenReturn(Optional.of(current));
        when(dependencies.addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(1L))
                .thenReturn(List.of(current));

        CustomerAddress saved = dependencies.service.update(10L, 1L, request(false));

        assertTrue(saved.isDefaultAddress());
    }

    @Test
    void settingAnotherDefaultClearsPreviousDefault() {
        Dependencies dependencies = dependencies();
        CustomerAddress previous = address(10L, true);
        CustomerAddress next = address(11L, false);
        when(dependencies.addresses.findById(11L)).thenReturn(Optional.of(next));
        when(dependencies.addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(1L))
                .thenReturn(List.of(previous, next));

        CustomerAddress saved = dependencies.service.update(11L, 1L, request(true));

        assertFalse(previous.isDefaultAddress());
        assertTrue(saved.isDefaultAddress());
    }

    @Test
    void deletingDefaultPromotesFirstRemainingAddress() {
        Dependencies dependencies = dependencies();
        CustomerAddress deleted = address(10L, true);
        CustomerAddress replacement = address(11L, false);
        when(dependencies.addresses.findById(10L)).thenReturn(Optional.of(deleted));
        when(dependencies.addresses.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(1L))
                .thenReturn(List.of(replacement));

        dependencies.service.delete(10L, 1L);

        assertTrue(replacement.isDefaultAddress());
        verify(dependencies.addresses).flush();
        verify(dependencies.addresses).save(replacement);
    }

    private Dependencies dependencies() {
        CustomerAddressRepository addresses = mock(CustomerAddressRepository.class);
        CustomerUserRepository customers = mock(CustomerUserRepository.class);
        when(customers.findByIdForUpdate(1L)).thenReturn(Optional.of(mock(CustomerUser.class)));
        when(addresses.save(org.mockito.ArgumentMatchers.any(CustomerAddress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return new Dependencies(new AddressService(addresses, customers), addresses);
    }

    private CustomerAddress address(Long id, boolean defaultAddress) {
        CustomerAddress address = new CustomerAddress();
        address.setId(id);
        address.setCustomerId(1L);
        address.setRecipientName("测试收货人");
        address.setRecipientPhone("13800138000");
        address.setDetailAddress("测试地址");
        address.setDefaultAddress(defaultAddress);
        return address;
    }

    private OrderDtos.AddressRequest request(boolean defaultAddress) {
        OrderDtos.AddressRequest request = new OrderDtos.AddressRequest();
        request.setRecipientName("测试收货人");
        request.setRecipientPhone("13800138000");
        request.setDetailAddress("测试地址");
        request.setDefaultAddress(defaultAddress);
        return request;
    }

    private record Dependencies(AddressService service, CustomerAddressRepository addresses) {
    }
}
