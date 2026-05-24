package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.CustomerAddress;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.OrderItem;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.entity.PointsTransaction;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.enums.OrderStatus;
import com.mall.pointsmall.enums.PointsTransactionType;
import com.mall.pointsmall.enums.PointsActorType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.OrderItemRepository;
import com.mall.pointsmall.repository.OrderMainRepository;
import com.mall.pointsmall.repository.ProductRepository;
import jakarta.transaction.Transactional;
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
    private final ProductTransactionService productTransactionService;

    public OrderService(OrderMainRepository orderMainRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        CustomerUserRepository customerUserRepository,
                        AddressService addressService,
                        PointsService pointsService,
                        NotificationService notificationService,
                        ProductTransactionService productTransactionService) {
        this.orderMainRepository = orderMainRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.customerUserRepository = customerUserRepository;
        this.addressService = addressService;
        this.pointsService = pointsService;
        this.notificationService = notificationService;
        this.productTransactionService = productTransactionService;
    }

    @Transactional
    public OrderMain checkout(Long customerId, OrderDtos.CheckoutRequest request) {
        CustomerUser user = customerUserRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("客户不存在"));
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
        int balanceBefore = pointsService.balanceOf(customerId);
        if (balanceBefore < totalPoints) {
            throw new BusinessException("积分不足");
        }
        OrderMain order = new OrderMain();
        order.setOrderNo("PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setCustomerId(customerId);
        order.setCustomerName(user.getName());
        order.setCustomerPhone(user.getPhone());
        order.setCustomerIdCardNo(user.getIdCardNo());
        order.setTotalPoints(totalPoints);
        order.setBalanceBefore(balanceBefore);
        order.setBalanceAfter(balanceBefore - totalPoints);
        order.setRecipientName(address.getRecipientName());
        order.setRecipientPhone(address.getRecipientPhone());
        order.setRecipientAddress(address.getDetailAddress());
        OrderMain saved = orderMainRepository.save(order);
        pointsService.changeForCustomer(customerId, -totalPoints, PointsTransactionType.ORDER_DEDUCT,
                saved.getId(), "客户兑换下单扣减积分", user.getName());
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderDtos.CheckoutItem checkoutItem = request.getItems().get(i);
            Product product = products.get(i);
            int stockBefore = product.getStock();
            product.setStock(stockBefore - checkoutItem.getQuantity());
            productRepository.save(product);
            productTransactionService.customerOrder(product, stockBefore, product.getStock(),
                    saved.getId(), customerId, user.getName());

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
        OrderMain order = ownedOrder(orderId, customerId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException("当前订单不可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderMainRepository.save(order);
    }

    @Transactional
    public int customerCancel(Long orderId, Long customerId) {
        OrderMain order = ownedOrder(orderId, customerId);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("仅待发货订单可以取消");
        }
        return cancelOrder(order, OrderStatus.MANUAL_CANCELLED).getBalanceAfter();
    }

    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        for (OrderMain order : orderMainRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_SHIPMENT, deadline)) {
            cancelOrder(order, OrderStatus.AUTO_CANCELLED);
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

    private PointsTransaction cancelOrder(OrderMain order, OrderStatus status) {
        order.setStatus(status);
        order.setCancelledAt(LocalDateTime.now());
        orderMainRepository.save(order);
        for (OrderItem item : orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("订单商品不存在"));
            int stockBefore = product.getStock();
            product.setStock(stockBefore + item.getQuantity());
            productRepository.save(product);
            productTransactionService.returnOrder(product, stockBefore, product.getStock(), order.getId(),
                    status == OrderStatus.MANUAL_CANCELLED ? PointsActorType.CUSTOMER : PointsActorType.SYSTEM,
                    status == OrderStatus.MANUAL_CANCELLED ? order.getCustomerId() : null,
                    status == OrderStatus.MANUAL_CANCELLED ? order.getCustomerName() : "系统自动处理",
                    status == OrderStatus.MANUAL_CANCELLED ? "客户取消订单返还库存" : "超时自动取消返还库存");
        }
        PointsTransaction refund = status == OrderStatus.MANUAL_CANCELLED
                ? pointsService.changeForCustomer(order.getCustomerId(), order.getTotalPoints(), PointsTransactionType.ORDER_REFUND,
                order.getId(), "客户手动取消订单返还积分", order.getCustomerName())
                : pointsService.changeBySystem(order.getCustomerId(), order.getTotalPoints(), PointsTransactionType.ORDER_REFUND,
                order.getId(), "待发货超时自动取消返还积分");
        notificationService.createOrderCancelled(order, status == OrderStatus.AUTO_CANCELLED, refund.getBalanceAfter());
        return refund;
    }

    private OrderMain ownedOrder(Long id, Long customerId) {
        OrderMain order = getOrder(id);
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    private OrderMain getOrder(Long id) {
        return orderMainRepository.findById(id).orElseThrow(() -> new BusinessException("订单不存在"));
    }
}
