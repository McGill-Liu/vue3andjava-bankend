package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.PointsAccount;
import com.mall.pointsmall.entity.PointsTransaction;
import com.mall.pointsmall.enums.PointsTransactionType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.PointsAccountRepository;
import com.mall.pointsmall.repository.PointsTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointsService {
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsTransactionRepository pointsTransactionRepository;

    public PointsService(PointsAccountRepository pointsAccountRepository,
                         PointsTransactionRepository pointsTransactionRepository) {
        this.pointsAccountRepository = pointsAccountRepository;
        this.pointsTransactionRepository = pointsTransactionRepository;
    }

    public List<PointsAccount> listAccounts() {
        return pointsAccountRepository.findAll();
    }

    public List<PointsTransaction> transactions(Long customerId) {
        return customerId == null ? pointsTransactionRepository.findAll()
                : pointsTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void adjust(AdminDtos.PointsAdjustmentRequest request) {
        changePoints(request.getCustomerId(), request.getAmount(), PointsTransactionType.ADMIN_ADJUST, null, request.getRemark());
    }

    @Transactional
    public void changePoints(Long customerId, int amount, PointsTransactionType type, Long orderId, String remark) {
        PointsAccount account = pointsAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("积分账户不存在"));
        int nextBalance = account.getBalance() + amount;
        if (nextBalance < 0) {
            throw new BusinessException("积分不足");
        }
        account.setBalance(nextBalance);
        pointsAccountRepository.save(account);
        PointsTransaction transaction = new PointsTransaction();
        transaction.setCustomerId(customerId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setOrderId(orderId);
        transaction.setRemark(remark);
        transaction.setBalanceAfter(nextBalance);
        pointsTransactionRepository.save(transaction);
    }

    @Transactional
    public void initialize(Long customerId, int amount, String remark) {
        PointsAccount account = pointsAccountRepository.findByCustomerId(customerId).orElseGet(() -> {
            PointsAccount created = new PointsAccount();
            created.setCustomerId(customerId);
            created.setBalance(0);
            return created;
        });
        account.setBalance(amount);
        pointsAccountRepository.save(account);
        PointsTransaction transaction = new PointsTransaction();
        transaction.setCustomerId(customerId);
        transaction.setType(PointsTransactionType.ADMIN_INIT);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(amount);
        transaction.setRemark(remark);
        pointsTransactionRepository.save(transaction);
    }
}
