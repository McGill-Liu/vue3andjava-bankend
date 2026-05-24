package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.enums.UserStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public List<AdminDtos.CustomerResponse> listAll() {
        return customerUserRepository.findAll().stream().map(this::response).toList();
    }

    public AdminDtos.CustomerResponse getById(Long id) {
        return response(requireUser(id));
    }

    @Transactional
    public AdminDtos.CustomerResponse create(AdminDtos.CustomerCreateRequest request, SecurityUser actor) {
        assertAvailable(request.getPhone(), request.getIdCardNo(), null);
        CustomerUser user = new CustomerUser();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIdCardNo(request.getIdCardNo());
        user.setPasswordHash(passwordEncoder.encode(defaultPassword(request.getIdCardNo())));
        user.setStatus(UserStatus.ACTIVE);
        CustomerUser saved = customerUserRepository.save(user);
        pointsService.initialize(saved.getId(), request.getInitialPoints(), actor);
        return response(saved);
    }

    @Transactional
    public AdminDtos.CustomerResponse update(Long id, AdminDtos.CustomerUpdateRequest request) {
        CustomerUser user = requireUser(id);
        assertAvailable(request.getPhone(), request.getIdCardNo(), id);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIdCardNo(request.getIdCardNo());
        try {
            user.setStatus(UserStatus.valueOf(request.getStatus()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("客户状态无效");
        }
        return response(customerUserRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id) {
        CustomerUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(defaultPassword(user.getIdCardNo())));
        customerUserRepository.save(user);
    }

    @Transactional
    public AdminDtos.CustomerResponse setBalance(Long id, AdminDtos.CustomerBalanceRequest request, SecurityUser actor) {
        CustomerUser user = requireUser(id);
        pointsService.setBalance(user.getId(), request.getTargetBalance(), request.getRemark(), actor);
        return response(user);
    }

    private CustomerUser requireUser(Long id) {
        return customerUserRepository.findById(id).orElseThrow(() -> new BusinessException("客户不存在"));
    }

    private AdminDtos.CustomerResponse response(CustomerUser user) {
        AdminDtos.CustomerResponse result = new AdminDtos.CustomerResponse();
        result.setId(user.getId());
        result.setName(user.getName());
        result.setPhone(user.getPhone());
        result.setIdCardNo(user.getIdCardNo());
        result.setStatus(user.getStatus().name());
        result.setPointsBalance(pointsService.balanceOf(user.getId()));
        return result;
    }

    private void assertAvailable(String phone, String idCardNo, Long editingId) {
        customerUserRepository.findByPhone(phone).filter(user -> !user.getId().equals(editingId)).ifPresent(user -> {
            throw new BusinessException("手机号已存在");
        });
        customerUserRepository.findAll().stream()
                .filter(user -> user.getIdCardNo().equals(idCardNo) && !user.getId().equals(editingId))
                .findFirst()
                .ifPresent(user -> {
                    throw new BusinessException("身份证号已存在");
                });
    }

    private String defaultPassword(String idCardNo) {
        if (idCardNo.length() < 6) {
            throw new BusinessException("身份证号至少需要 6 位");
        }
        return idCardNo.substring(idCardNo.length() - 6);
    }
}
