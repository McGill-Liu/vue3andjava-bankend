package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerAddressRepository;
import com.mall.pointsmall.repository.CustomerUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private final CustomerAddressRepository addressRepository;
    private final CustomerUserRepository customerUserRepository;

    public AddressService(CustomerAddressRepository addressRepository,
                          CustomerUserRepository customerUserRepository) {
        this.addressRepository = addressRepository;
        this.customerUserRepository = customerUserRepository;
    }

    public List<CustomerAddress> list(Long customerId) {
        return addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId);
    }

    public CustomerAddress getOwned(Long id, Long customerId) {
        CustomerAddress address = addressRepository.findById(id).orElseThrow(() -> new BusinessException("地址不存在"));
        if (!address.getCustomerId().equals(customerId)) {
            throw new BusinessException("无权访问该地址");
        }
        return address;
    }

    @Transactional
    public CustomerAddress create(Long customerId, OrderDtos.AddressRequest request) {
        lockCustomer(customerId);
        List<CustomerAddress> existing = list(customerId);
        boolean shouldBeDefault = request.isDefaultAddress() || existing.isEmpty()
                || existing.stream().noneMatch(CustomerAddress::isDefaultAddress);
        if (shouldBeDefault) {
            clearDefault(existing);
        }
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        copy(request, address);
        address.setDefaultAddress(shouldBeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public CustomerAddress update(Long id, Long customerId, OrderDtos.AddressRequest request) {
        lockCustomer(customerId);
        CustomerAddress address = getOwned(id, customerId);
        List<CustomerAddress> addresses = list(customerId);
        boolean hasOtherDefault = addresses.stream()
                .anyMatch(item -> !item.getId().equals(id) && item.isDefaultAddress());
        boolean shouldBeDefault = request.isDefaultAddress() || !hasOtherDefault;
        if (shouldBeDefault) {
            clearDefault(addresses);
        }
        copy(request, address);
        address.setDefaultAddress(shouldBeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public void delete(Long id, Long customerId) {
        lockCustomer(customerId);
        addressRepository.delete(getOwned(id, customerId));
        addressRepository.flush();
        ensureDefault(customerId);
    }

    private void copy(OrderDtos.AddressRequest request, CustomerAddress address) {
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setDetailAddress(request.getDetailAddress());
        address.setDefaultAddress(request.isDefaultAddress());
    }

    private void clearDefault(List<CustomerAddress> addresses) {
        for (CustomerAddress address : addresses) {
            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
                addressRepository.save(address);
            }
        }
    }

    private void ensureDefault(Long customerId) {
        List<CustomerAddress> addresses = list(customerId);
        if (!addresses.isEmpty() && addresses.stream().noneMatch(CustomerAddress::isDefaultAddress)) {
            CustomerAddress replacement = addresses.get(0);
            replacement.setDefaultAddress(true);
            addressRepository.save(replacement);
        }
    }

    private void lockCustomer(Long customerId) {
        customerUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessException("客户不存在"));
    }
}
