package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.enums.OrderStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.OrderItemRepository;
import com.mall.pointsmall.repository.OrderMainRepository;
import com.mall.pointsmall.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    @Test
    void rejectsInvalidQuantityInsideServiceAsDefenseInDepth() {
        Dependencies dependencies = dependencies();
        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(-1);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_invalid_quantity");
        request.setAddressId(3L);
        request.setItems(List.of(item));

        assertThrows(BusinessException.class, () -> dependencies.service.checkout(1L, request));
        verifyNoInteractions(dependencies.customers, dependencies.addresses, dependencies.products);
    }

    @Test
    void customerCannotReadAnotherCustomersOrderItems() {
        Dependencies dependencies = dependencies();
        OrderMain order = new OrderMain();
        order.setId(10L);
        order.setCustomerId(99L);
        when(dependencies.orders.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> dependencies.service.ownedOrderItems(10L, 1L));
        verifyNoInteractions(dependencies.orderItems);
    }

    @Test
    void rejectsCheckoutWhenConfirmedTotalNoLongerMatchesCurrentProductPrice() {
        Dependencies dependencies = dependencies();
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setName("测试商品");
        product.setEnabled(true);
        product.setPointsCost(10);
        product.setStock(5);

        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(1);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_price_change");
        request.setAddressId(3L);
        request.setItems(List.of(item));
        request.setExpectedTotalPoints(9);

        when(dependencies.customers.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(dependencies.addresses.getOwned(3L, 1L)).thenReturn(mock(CustomerAddress.class));
        when(dependencies.products.findAllByIdForUpdate(List.of(2L))).thenReturn(List.of(product));

        assertThrows(BusinessException.class, () -> dependencies.service.checkout(1L, request));
        verifyNoInteractions(dependencies.points);
    }

    @Test
    void shippingTrimsLogisticsValuesAndChangesPendingOrderOnly() {
        Dependencies dependencies = dependencies();
        OrderMain order = new OrderMain();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_SHIPMENT);
        AdminDtos.ShipOrderRequest request = new AdminDtos.ShipOrderRequest();
        request.setShippingCompany("  顺丰速运  ");
        request.setShippingNo("  SF123456789  ");
        when(dependencies.orders.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(dependencies.orders.save(order)).thenReturn(order);

        OrderMain shipped = dependencies.service.ship(10L, request);

        assertEquals(OrderStatus.SHIPPED, shipped.getStatus());
        assertEquals("顺丰速运", shipped.getShippingCompany());
        assertEquals("SF123456789", shipped.getShippingNo());
        assertNotNull(shipped.getShippedAt());
    }

    @Test
    void rejectsCheckoutQuantityAboveProductPurchaseLimit() {
        Dependencies dependencies = dependencies();
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setName("限购商品");
        product.setEnabled(true);
        product.setPointsCost(10);
        product.setStock(10);
        product.setPerOrderLimit(1);

        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(2);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_order_limit");
        request.setAddressId(3L);
        request.setItems(List.of(item));

        when(dependencies.customers.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(dependencies.addresses.getOwned(3L, 1L)).thenReturn(mock(CustomerAddress.class));
        when(dependencies.products.findAllByIdForUpdate(List.of(2L))).thenReturn(List.of(product));

        assertThrows(BusinessException.class, () -> dependencies.service.checkout(1L, request));
        verifyNoInteractions(dependencies.points);
    }

    @Test
    void rejectsCheckoutAboveCustomerCumulativePurchaseLimit() {
        Dependencies dependencies = dependencies();
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setName("累计限购商品");
        product.setEnabled(true);
        product.setPointsCost(10);
        product.setStock(10);
        product.setCustomerTotalLimit(5);

        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(2);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_total_limit");
        request.setAddressId(3L);
        request.setItems(List.of(item));

        OrderItemRepository.CustomerProductPurchaseTotal purchaseTotal = mock(OrderItemRepository.CustomerProductPurchaseTotal.class);
        when(purchaseTotal.getProductId()).thenReturn(2L);
        when(purchaseTotal.getPurchasedQuantity()).thenReturn(4L);
        when(dependencies.customers.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(dependencies.addresses.getOwned(3L, 1L)).thenReturn(mock(CustomerAddress.class));
        when(dependencies.products.findAllByIdForUpdate(List.of(2L))).thenReturn(List.of(product));
        when(dependencies.orderItems.sumCustomerEffectivePurchases(eq(1L), eq(List.of(2L)), anyList()))
                .thenReturn(List.of(purchaseTotal));

        BusinessException error = assertThrows(BusinessException.class, () -> dependencies.service.checkout(1L, request));
        assertEquals("累计限购商品 每位客户累计限购 5 件，您已购买 4 件，本次最多还能购买 1 件", error.getMessage());
        verifyNoInteractions(dependencies.points);
    }

    @Test
    void repeatedCheckoutTokenReturnsExistingOrderWithoutRepeatingBusinessChanges() {
        Dependencies dependencies = dependencies();
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        OrderMain existingOrder = new OrderMain();
        existingOrder.setId(20L);
        existingOrder.setCustomerId(1L);
        existingOrder.setCheckoutToken("checkout_test_retry_123");

        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(1);
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_retry_123");
        request.setAddressId(3L);
        request.setItems(List.of(item));

        when(dependencies.customers.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(dependencies.orders.findByCustomerIdAndCheckoutToken(1L, "checkout_test_retry_123"))
                .thenReturn(Optional.of(existingOrder));

        assertEquals(existingOrder, dependencies.service.checkout(1L, request));
        verifyNoInteractions(dependencies.addresses, dependencies.products, dependencies.points,
                dependencies.orderItems);
    }

    private Dependencies dependencies() {
        OrderMainRepository orders = mock(OrderMainRepository.class);
        OrderItemRepository orderItems = mock(OrderItemRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        CustomerUserRepository customers = mock(CustomerUserRepository.class);
        AddressService addresses = mock(AddressService.class);
        PointsService points = mock(PointsService.class);
        OrderService service = new OrderService(orders, orderItems, products, customers, addresses,
                points, mock(NotificationService.class), mock(ProductTransactionService.class));
        return new Dependencies(service, orders, orderItems, products, customers, addresses, points);
    }

    private record Dependencies(OrderService service, OrderMainRepository orders, OrderItemRepository orderItems,
                                ProductRepository products, CustomerUserRepository customers, AddressService addresses,
                                PointsService points) {
    }
}
