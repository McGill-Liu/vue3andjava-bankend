package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.enums.UserStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerUserService {
    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PointsService pointsService;

    public CustomerUserService(CustomerUserRepository customerUserRepository,
                               PasswordEncoder passwordEncoder,
                               PointsService pointsService) {
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.pointsService = pointsService;
    }

    public List<CustomerUser> listAll() {
        return customerUserRepository.findAll();
    }

    public List<CustomerUser> pendingApprovals() {
        return customerUserRepository.findByStatus(UserStatus.PENDING_APPROVAL);
    }

    public CustomerUser getById(Long id) {
        return customerUserRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void approve(Long id, Integer initialPoints, SecurityUser approver) {
        CustomerUser user = getById(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovedAt(LocalDateTime.now());
        user.setApprovedBy(approver.getId());
        customerUserRepository.save(user);
        pointsService.initialize(id, initialPoints, "审核通过初始化积分");
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void reject(Long id) {
        CustomerUser user = getById(id);
        user.setStatus(UserStatus.REJECTED);
        customerUserRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CustomerUser updatePhone(Long id, AdminDtos.UpdatePhoneRequest request) {
        CustomerUser user = getById(id);
        user.setPhone(request.getPhone());
        return customerUserRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CustomerUser updateIdCard(Long id, AdminDtos.UpdateIdCardRequest request) {
        CustomerUser user = getById(id);
        user.setIdCardNo(request.getIdCardNo());
        return customerUserRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void updatePassword(Long id, AdminDtos.UpdatePasswordRequest request) {
        CustomerUser user = getById(id);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customerUserRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CustomerUser updateStatus(Long id, AdminDtos.UpdateStatusRequest request) {
        CustomerUser user = getById(id);
        user.setStatus(UserStatus.valueOf(request.getStatus()));
        return customerUserRepository.save(user);
    }
}
