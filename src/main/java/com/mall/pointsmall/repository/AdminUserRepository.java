package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByPhone(String phone);
    List<AdminUser> findByEnabledTrue();
}
