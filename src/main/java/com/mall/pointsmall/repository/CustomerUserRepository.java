package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.CustomerUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByIdCardNo(String idCardNo);
}
