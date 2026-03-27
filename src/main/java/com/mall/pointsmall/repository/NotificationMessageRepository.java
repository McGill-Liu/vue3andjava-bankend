package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.NotificationMessage;
import com.mall.pointsmall.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    long countByStatus(NotificationStatus status);
    List<NotificationMessage> findAllByOrderByCreatedAtDesc();
}
