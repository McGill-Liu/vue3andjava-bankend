package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(Long customerId);
    List<CustomerAddress> findByCustomerIdInOrderByCustomerIdAscDefaultAddressDescCreatedAtDesc(List<Long> customerIds);
}
