package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.OrderItem;
import com.mall.pointsmall.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
            select item.productId as productId, sum(item.quantity) as purchasedQuantity
            from OrderItem item, OrderMain orderEntity
            where item.orderId = orderEntity.id
              and orderEntity.customerId = :customerId
              and orderEntity.status in :statuses
              and item.productId in :productIds
            group by item.productId
            """)
    List<CustomerProductPurchaseTotal> sumCustomerEffectivePurchases(@Param("customerId") Long customerId,
                                                                      @Param("productIds") List<Long> productIds,
                                                                      @Param("statuses") List<OrderStatus> statuses);

    interface CustomerProductPurchaseTotal {
        Long getProductId();
        Long getPurchasedQuantity();
    }
}
