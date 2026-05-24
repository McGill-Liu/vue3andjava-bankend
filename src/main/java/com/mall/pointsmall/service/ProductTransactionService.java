package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductTransaction;
import com.mall.pointsmall.enums.PointsActorType;
import com.mall.pointsmall.enums.ProductTransactionType;
import com.mall.pointsmall.repository.ProductTransactionRepository;
import com.mall.pointsmall.security.SecurityUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductTransactionService {
    private final ProductTransactionRepository repository;

    public ProductTransactionService(ProductTransactionRepository repository) {
        this.repository = repository;
    }

    public List<ProductTransaction> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public void adminChange(Product product, ProductTransactionType type, int before, int after, SecurityUser actor, String remark) {
        save(product, type, before, after, null, PointsActorType.ADMIN, actor.getId(), actor.getName(), remark);
    }

    public void customerOrder(Product product, int before, int after, Long orderId, Long customerId, String customerName) {
        save(product, ProductTransactionType.ORDER_DEDUCT, before, after, orderId,
                PointsActorType.CUSTOMER, customerId, customerName, "客户下单扣减库存");
    }

    public void returnOrder(Product product, int before, int after, Long orderId,
                            PointsActorType actorType, Long actorId, String actorName, String remark) {
        save(product, ProductTransactionType.ORDER_RETURN, before, after, orderId, actorType, actorId, actorName, remark);
    }

    private void save(Product product, ProductTransactionType type, int before, int after, Long orderId,
                      PointsActorType actorType, Long actorId, String actorName, String remark) {
        ProductTransaction transaction = new ProductTransaction();
        transaction.setProductId(product.getId());
        transaction.setProductName(product.getName());
        transaction.setType(type);
        transaction.setQuantityChange(after - before);
        transaction.setStockBefore(before);
        transaction.setStockAfter(after);
        transaction.setOrderId(orderId);
        transaction.setActorType(actorType);
        transaction.setActorId(actorId);
        transaction.setActorName(actorName);
        transaction.setRemark(remark);
        repository.save(transaction);
    }
}
