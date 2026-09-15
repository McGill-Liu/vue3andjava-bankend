package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.CustomerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByPhone(String phone);

    Optional<CustomerUser> findByWechatOpenId(String wechatOpenId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from CustomerUser user where user.phone = :phone")
    Optional<CustomerUser> findByPhoneForUpdate(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from CustomerUser user where user.id = :id")
    Optional<CustomerUser> findByIdForUpdate(Long id);

    boolean existsByPhone(String phone);
    boolean existsByIdCardNo(String idCardNo);
}
