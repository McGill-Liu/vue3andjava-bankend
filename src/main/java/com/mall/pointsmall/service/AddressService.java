package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerAddressRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private final CustomerAddressRepository addressRepository;

    public AddressService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
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
        if (request.isDefaultAddress()) {
            clearDefault(customerId);
        }
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        copy(request, address);
        return addressRepository.save(address);
    }

    @Transactional
    public CustomerAddress update(Long id, Long customerId, OrderDtos.AddressRequest request) {
        CustomerAddress address = getOwned(id, customerId);
        if (request.isDefaultAddress()) {
            clearDefault(customerId);
        }
        copy(request, address);
        return addressRepository.save(address);
    }

    @Transactional
    public void delete(Long id, Long customerId) {
        addressRepository.delete(getOwned(id, customerId));
    }

    private void copy(OrderDtos.AddressRequest request, CustomerAddress address) {
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setDetailAddress(request.getDetailAddress());
        address.setDefaultAddress(request.isDefaultAddress());
    }

    private void clearDefault(Long customerId) {
        List<CustomerAddress> addresses = addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId);
        for (CustomerAddress address : addresses) {
            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
                addressRepository.save(address);
            }
        }
    }
}
