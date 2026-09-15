package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.OrderItem;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RepositoryQueryTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointsAccountRepository pointsAccountRepository;

    @Autowired
    private OrderMainRepository orderMainRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void lockingQueriesAreValidAtRepositoryStartup() {
        assertNotNull(productRepository);
        assertNotNull(pointsAccountRepository);
        assertNotNull(orderMainRepository);
    }

    @Test
    void cumulativePurchaseQueryExcludesCancelledOrders() {
        OrderMain effectiveOrder = orderMainRepository.save(order(7L, "PO-EFFECTIVE", OrderStatus.PENDING_SHIPMENT));
        OrderMain cancelledOrder = orderMainRepository.save(order(7L, "PO-CANCELLED", OrderStatus.MANUAL_CANCELLED));
        orderItemRepository.save(item(effectiveOrder.getId(), 11L, 2));
        orderItemRepository.save(item(cancelledOrder.getId(), 11L, 5));

        List<OrderItemRepository.CustomerProductPurchaseTotal> totals = orderItemRepository.sumCustomerEffectivePurchases(
                7L, List.of(11L), List.of(OrderStatus.PENDING_SHIPMENT, OrderStatus.SHIPPED, OrderStatus.COMPLETED));

        assertEquals(1, totals.size());
        assertEquals(11L, totals.get(0).getProductId());
        assertEquals(2L, totals.get(0).getPurchasedQuantity());
    }

    private OrderMain order(Long customerId, String orderNo, OrderStatus status) {
        OrderMain order = new OrderMain();
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setCustomerName("测试客户");
        order.setCustomerPhone("13100000000");
        order.setCustomerIdCardNo("TEST-ID");
        order.setTotalPoints(10);
        order.setBalanceBefore(100);
        order.setBalanceAfter(90);
        order.setStatus(status);
        order.setRecipientName("测试客户");
        order.setRecipientPhone("13100000000");
        order.setRecipientAddress("测试地址");
        return order;
    }

    private OrderItem item(Long orderId, Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName("测试商品");
        item.setProductCoverImage("");
        item.setPointsCost(5);
        item.setQuantity(quantity);
        return item;
    }
}
