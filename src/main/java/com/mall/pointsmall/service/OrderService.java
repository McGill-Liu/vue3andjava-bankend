package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.OrderItem;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.enums.OrderStatus;
import com.mall.pointsmall.enums.PointsTransactionType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.OrderItemRepository;
import com.mall.pointsmall.repository.OrderMainRepository;
import com.mall.pointsmall.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final OrderMainRepository orderMainRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerUserRepository customerUserRepository;
    private final AddressService addressService;
    private final PointsService pointsService;
    private final NotificationService notificationService;

    public OrderService(OrderMainRepository orderMainRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        CustomerUserRepository customerUserRepository,
                        AddressService addressService,
                        PointsService pointsService,
                        NotificationService notificationService) {
        this.orderMainRepository = orderMainRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.customerUserRepository = customerUserRepository;
        this.addressService = addressService;
        this.pointsService = pointsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderMain checkout(Long customerId, OrderDtos.CheckoutRequest request) {
        CustomerUser user = customerUserRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        CustomerAddress address = addressService.getOwned(request.getAddressId(), customerId);
        int totalPoints = 0;
        List<Product> products = new ArrayList<>();
        for (OrderDtos.CheckoutItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            if (!product.isEnabled()) {
                throw new BusinessException(product.getName() + " 已下架");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException(product.getName() + " 库存不足");
            }
            totalPoints += product.getPointsCost() * item.getQuantity();
            products.add(product);
        }
        OrderMain order = new OrderMain();
        order.setOrderNo("PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        order.setCustomerId(customerId);
        order.setCustomerName(user.getName());
        order.setCustomerPhone(user.getPhone());
        order.setTotalPoints(totalPoints);
        order.setRecipientName(address.getRecipientName());
        order.setRecipientPhone(address.getRecipientPhone());
        order.setRecipientAddress(address.getDetailAddress());
        OrderMain saved = orderMainRepository.save(order);
        pointsService.changePoints(customerId, -totalPoints, PointsTransactionType.ORDER_DEDUCT, saved.getId(), "下单扣减积分");
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderDtos.CheckoutItem checkoutItem = request.getItems().get(i);
            Product product = products.get(i);
            product.setStock(product.getStock() - checkoutItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(saved.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductCoverImage(product.getCoverImageUrl());
            orderItem.setPointsCost(product.getPointsCost());
            orderItem.setQuantity(checkoutItem.getQuantity());
            orderItemRepository.save(orderItem);
        }
        notificationService.createOrderCreated(saved);
        return saved;
    }

    public List<OrderMain> userOrders(Long customerId) {
        return orderMainRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<OrderMain> adminOrders() {
        return orderMainRepository.findAll();
    }

    public List<OrderItem> orderItems(Long orderId) {
        return orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    public void ship(Long orderId, AdminDtos.ShipOrderRequest request) {
        OrderMain order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("当前订单不可发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippingCompany(request.getShippingCompany());
        order.setShippingNo(request.getShippingNo());
        order.setShippedAt(LocalDateTime.now());
        orderMainRepository.save(order);
    }

    @Transactional
    public void confirm(Long orderId, Long customerId) {
        OrderMain order = getOrder(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException("当前订单不可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderMainRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    public void cancel(Long orderId) {
        OrderMain order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("仅待发货订单可取消");
        }
        cancelOrder(order);
    }

    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        for (OrderMain order : orderMainRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_SHIPMENT, deadline)) {
            cancelOrder(order);
        }
    }

    @Transactional
    public void autoCompleteShippedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        for (OrderMain order : orderMainRepository.findByStatusAndShippedAtBefore(OrderStatus.SHIPPED, deadline)) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            orderMainRepository.save(order);
        }
    }

    private void cancelOrder(OrderMain order) {
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderMainRepository.save(order);
        for (OrderItem item : orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        pointsService.changePoints(order.getCustomerId(), order.getTotalPoints(), PointsTransactionType.ORDER_REFUND, order.getId(), "订单取消退回积分");
        notificationService.createOrderCancelled(order);
    }

    private OrderMain getOrder(Long id) {
        return orderMainRepository.findById(id).orElseThrow(() -> new BusinessException("订单不存在"));
    }
}
