package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByPhone(String phone);
    Optional<CustomerUser> findByPhoneAndNameAndIdCardNo(String phone, String name, String idCardNo);
    boolean existsByPhone(String phone);
    boolean existsByIdCardNo(String idCardNo);
    List<CustomerUser> findByStatus(UserStatus status);
}
