package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.PointsAccount;
import com.mall.pointsmall.entity.PointsTransaction;
import com.mall.pointsmall.enums.PointsActorType;
import com.mall.pointsmall.enums.PointsTransactionType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.PointsAccountRepository;
import com.mall.pointsmall.repository.PointsTransactionRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointsService {
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsTransactionRepository pointsTransactionRepository;
    private final CustomerUserRepository customerUserRepository;

    public PointsService(PointsAccountRepository pointsAccountRepository,
                         PointsTransactionRepository pointsTransactionRepository,
                         CustomerUserRepository customerUserRepository) {
        this.pointsAccountRepository = pointsAccountRepository;
        this.pointsTransactionRepository = pointsTransactionRepository;
        this.customerUserRepository = customerUserRepository;
    }

    public int balanceOf(Long customerId) {
        return pointsAccountRepository.findByCustomerId(customerId).map(PointsAccount::getBalance).orElse(0);
    }

    public int balanceForUpdate(Long customerId) {
        return requireAccountForUpdate(customerId).getBalance();
    }

    public List<PointsTransaction> transactions(Long customerId) {
        return customerId == null ? pointsTransactionRepository.findAllByOrderByCreatedAtDesc()
                : pointsTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public PointsTransaction initialize(Long customerId, int amount, SecurityUser actor) {
        if (amount < 0) {
            throw new BusinessException("初始积分不能小于 0");
        }
        PointsAccount account = new PointsAccount();
        account.setCustomerId(customerId);
        account.setBalance(amount);
        pointsAccountRepository.save(account);
        return saveTransaction(customerId, PointsTransactionType.ADMIN_INIT, amount, 0, amount, null,
                "新增客户初始化积分", PointsActorType.ADMIN, actor.getId(), actor.getName());
    }

    @Transactional
    public PointsTransaction setBalance(Long customerId, int targetBalance, String remark, SecurityUser actor) {
        if (targetBalance < 0) {
            throw new BusinessException("目标积分不能小于 0");
        }
        PointsAccount account = requireAccountForUpdate(customerId);
        int before = account.getBalance();
        int amount;
        try {
            amount = Math.subtractExact(targetBalance, before);
        } catch (ArithmeticException ex) {
            throw new BusinessException("积分变动数值过大");
        }
        if (amount == 0) {
            throw new BusinessException("目标积分与当前积分相同");
        }
        account.setBalance(targetBalance);
        pointsAccountRepository.save(account);
        return saveTransaction(customerId, PointsTransactionType.ADMIN_ADJUST, amount, before, targetBalance, null,
                remark == null || remark.isBlank() ? "员工调整积分" : remark,
                PointsActorType.ADMIN, actor.getId(), actor.getName());
    }

    @Transactional
    public PointsTransaction changeForCustomer(Long customerId, int amount, PointsTransactionType type,
                                               Long orderId, String remark, String customerName) {
        return changePoints(customerId, amount, type, orderId, remark, PointsActorType.CUSTOMER, customerId, customerName);
    }

    @Transactional
    public PointsTransaction changeBySystem(Long customerId, int amount, PointsTransactionType type,
                                            Long orderId, String remark) {
        return changePoints(customerId, amount, type, orderId, remark, PointsActorType.SYSTEM, null, "系统自动处理");
    }

    private PointsTransaction changePoints(Long customerId, int amount, PointsTransactionType type, Long orderId,
                                           String remark, PointsActorType actorType, Long actorId, String actorName) {
        PointsAccount account = requireAccountForUpdate(customerId);
        int before = account.getBalance();
        int after;
        try {
            after = Math.addExact(before, amount);
        } catch (ArithmeticException ex) {
            throw new BusinessException("积分变动数值过大");
        }
        if (after < 0) {
            throw new BusinessException("积分不足");
        }
        account.setBalance(after);
        pointsAccountRepository.save(account);
        return saveTransaction(customerId, type, amount, before, after, orderId, remark, actorType, actorId, actorName);
    }

    private PointsAccount requireAccount(Long customerId) {
        return pointsAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("积分账户不存在"));
    }

    private PointsAccount requireAccountForUpdate(Long customerId) {
        return pointsAccountRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessException("积分账户不存在"));
    }

    private PointsTransaction saveTransaction(Long customerId, PointsTransactionType type, int amount,
                                              int balanceBefore, int balanceAfter, Long orderId, String remark,
                                              PointsActorType actorType, Long actorId, String actorName) {
        CustomerUser customer = customerUserRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("客户不存在"));
        PointsTransaction transaction = new PointsTransaction();
        transaction.setCustomerId(customerId);
        transaction.setCustomerName(customer.getName());
        transaction.setCustomerPhone(customer.getPhone());
        transaction.setCustomerIdCardNo(customer.getIdCardNo());
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setActorType(actorType);
        transaction.setActorId(actorId);
        transaction.setActorName(actorName);
        transaction.setOrderId(orderId);
        transaction.setRemark(remark);
        return pointsTransactionRepository.save(transaction);
    }
}
