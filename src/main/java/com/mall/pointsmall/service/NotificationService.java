package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.entity.NotificationMessage;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.enums.NotificationStatus;
import com.mall.pointsmall.enums.NotificationType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.NotificationMessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationMessageRepository notificationRepository;
    private final AdminUserRepository adminUserRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationMessageRepository notificationRepository,
                               AdminUserRepository adminUserRepository,
                               JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.adminUserRepository = adminUserRepository;
        this.mailSender = mailSender;
    }

    public List<NotificationMessage> list() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public long unprocessedCount() {
        return notificationRepository.countByStatus(NotificationStatus.UNPROCESSED);
    }

    @Transactional
    public NotificationMessage createOrderCreated(OrderMain order) {
        String content = "订单号: " + order.getOrderNo() + "\n用户: " + order.getCustomerName() + "\n手机号: " + order.getCustomerPhone()
                + "\n积分: " + order.getTotalPoints() + "\n请及时发货并同步外部积分 App 扣减。";
        NotificationMessage message = new NotificationMessage();
        message.setOrderId(order.getId());
        message.setCustomerId(order.getCustomerId());
        message.setType(NotificationType.ORDER_CREATED);
        message.setTitle("新订单待发货");
        message.setContent(content);
        NotificationMessage saved = notificationRepository.save(message);
        sendMail("积分商城新订单待处理", content);
        return saved;
    }

    @Transactional
    public NotificationMessage createOrderCancelled(OrderMain order) {
        String content = "订单号: " + order.getOrderNo() + "\n用户: " + order.getCustomerName() + "\n手机号: " + order.getCustomerPhone()
                + "\n积分返还: " + order.getTotalPoints() + "\n请同步外部积分 App 回加积分。";
        NotificationMessage message = new NotificationMessage();
        message.setOrderId(order.getId());
        message.setCustomerId(order.getCustomerId());
        message.setType(NotificationType.ORDER_CANCELLED);
        message.setTitle("订单取消待回积分");
        message.setContent(content);
        NotificationMessage saved = notificationRepository.save(message);
        sendMail("积分商城订单取消待处理", content);
        return saved;
    }

    @Transactional
    public void process(Long id, Long processedBy) {
        NotificationMessage message = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("通知不存在"));
        message.setStatus(NotificationStatus.PROCESSED);
        message.setProcessedAt(LocalDateTime.now());
        message.setProcessedBy(processedBy);
        notificationRepository.save(message);
    }

    private void sendMail(String subject, String content) {
        List<String> emails = adminUserRepository.findByEnabledTrue().stream().map(AdminUser::getEmail).collect(Collectors.toList());
        if (emails.isEmpty()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emails.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }
}
