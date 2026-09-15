package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.enums.UserStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.CustomerAddressRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerUserService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TEMP_PASSWORD_DAYS = 7;
    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PointsService pointsService;
    private final CustomerSessionService customerSessionService;
    private final CustomerAddressRepository customerAddressRepository;

    public CustomerUserService(CustomerUserRepository customerUserRepository,
                               PasswordEncoder passwordEncoder,
                               PointsService pointsService,
                               CustomerSessionService customerSessionService,
                               CustomerAddressRepository customerAddressRepository) {
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.pointsService = pointsService;
        this.customerSessionService = customerSessionService;
        this.customerAddressRepository = customerAddressRepository;
    }

    public List<AdminDtos.CustomerResponse> listAll() {
        List<CustomerUser> users = customerUserRepository.findAll();
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AdminDtos.CustomerAddressResponse>> addresses = customerAddressRepository
                .findByCustomerIdInOrderByCustomerIdAscDefaultAddressDescCreatedAtDesc(users.stream().map(CustomerUser::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(CustomerAddress::getCustomerId,
                        Collectors.mapping(this::addressResponse, Collectors.toList())));
        return users.stream().map(user -> response(user, addresses.getOrDefault(user.getId(), List.of()))).toList();
    }

    public AdminDtos.CustomerResponse getById(Long id) {
        return response(requireUser(id), addressesOf(id));
    }

    @Transactional
    public AdminDtos.CustomerTemporaryPasswordResponse create(AdminDtos.CustomerCreateRequest request, SecurityUser actor) {
        assertAvailable(request.getPhone(), request.getIdCardNo(), null);
        CustomerUser user = new CustomerUser();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIdCardNo(request.getIdCardNo());
        String temporaryPassword = temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        user.setTempPasswordExpiresAt(LocalDateTime.now().plusDays(TEMP_PASSWORD_DAYS));
        CustomerUser saved = customerUserRepository.save(user);
        pointsService.initialize(saved.getId(), request.getInitialPoints(), actor);
        return temporaryPasswordResponse(saved, temporaryPassword);
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
        CustomerUser saved = customerUserRepository.save(user);
        customerSessionService.logout(id);
        return response(saved, addressesOf(id));
    }

    @Transactional
    public AdminDtos.CustomerTemporaryPasswordResponse resetPassword(Long id) {
        CustomerUser user = requireUser(id);
        String temporaryPassword = temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setTempPasswordExpiresAt(LocalDateTime.now().plusDays(TEMP_PASSWORD_DAYS));
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        CustomerUser saved = customerUserRepository.save(user);
        customerSessionService.logout(user.getId());
        return temporaryPasswordResponse(saved, temporaryPassword);
    }

    @Transactional
    public AdminDtos.CustomerResponse setBalance(Long id, AdminDtos.CustomerBalanceRequest request, SecurityUser actor) {
        CustomerUser user = requireUser(id);
        pointsService.setBalance(user.getId(), request.getTargetBalance(), request.getRemark(), actor);
        return response(user, addressesOf(id));
    }

    private CustomerUser requireUser(Long id) {
        return customerUserRepository.findById(id).orElseThrow(() -> new BusinessException("客户不存在"));
    }

    private AdminDtos.CustomerResponse response(CustomerUser user, List<AdminDtos.CustomerAddressResponse> addresses) {
        AdminDtos.CustomerResponse result = new AdminDtos.CustomerResponse();
        result.setId(user.getId());
        result.setName(user.getName());
        result.setPhone(user.getPhone());
        result.setIdCardNo(user.getIdCardNo());
        result.setStatus(user.getStatus().name());
        result.setPointsBalance(pointsService.balanceOf(user.getId()));
        result.setMustChangePassword(user.isMustChangePassword());
        result.setTempPasswordExpiresAt(user.getTempPasswordExpiresAt() == null ? null : user.getTempPasswordExpiresAt().toString());
        result.setLoginLockedUntil(user.getLoginLockedUntil() == null ? null : user.getLoginLockedUntil().toString());
        result.setWechatBound(user.getWechatOpenId() != null);
        result.setAddresses(addresses);
        return result;
    }

    private List<AdminDtos.CustomerAddressResponse> addressesOf(Long customerId) {
        return customerAddressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId)
                .stream().map(this::addressResponse).toList();
    }

    private AdminDtos.CustomerAddressResponse addressResponse(CustomerAddress address) {
        AdminDtos.CustomerAddressResponse result = new AdminDtos.CustomerAddressResponse();
        result.setRecipientName(address.getRecipientName());
        result.setRecipientPhone(address.getRecipientPhone());
        result.setDetailAddress(address.getDetailAddress());
        result.setDefaultAddress(address.isDefaultAddress());
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

    private AdminDtos.CustomerTemporaryPasswordResponse temporaryPasswordResponse(CustomerUser user, String password) {
        AdminDtos.CustomerTemporaryPasswordResponse response = new AdminDtos.CustomerTemporaryPasswordResponse();
        response.setCustomerId(user.getId());
        response.setCustomerName(user.getName());
        response.setTemporaryPassword(password);
        response.setExpiresAt(user.getTempPasswordExpiresAt().toString());
        return response;
    }

    private String temporaryPassword() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

}
