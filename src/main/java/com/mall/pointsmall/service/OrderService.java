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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class OrderService {
    private static final List<OrderStatus> EFFECTIVE_PURCHASE_STATUSES = List.of(
            OrderStatus.PENDING_SHIPMENT, OrderStatus.SHIPPED, OrderStatus.COMPLETED);

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
        String checkoutToken = normalizeCheckoutToken(request.getCheckoutToken());
        TreeMap<Long, Integer> quantities = aggregateCheckoutItems(request.getItems());
        CustomerUser user = customerUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessException("客户不存在"));
        OrderMain existingOrder = orderMainRepository.findByCustomerIdAndCheckoutToken(customerId, checkoutToken)
                .orElse(null);
        if (existingOrder != null) {
            return existingOrder;
        }
        CustomerAddress address = addressService.getOwned(request.getAddressId(), customerId);
        List<Product> products = productRepository.findAllByIdForUpdate(new ArrayList<>(quantities.keySet()));
        if (products.size() != quantities.size()) {
            throw new BusinessException("商品不存在");
        }
        Map<Long, Product> productsById = new HashMap<>();
        Map<Long, Long> purchasedQuantities = effectivePurchaseTotals(customerId, new ArrayList<>(quantities.keySet()));
        long totalPointsLong = 0;
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            if (!product.isEnabled()) {
                throw new BusinessException(product.getName() + " 已下架");
            }
            if (product.getPointsCost() < 0 || product.getStock() < 0) {
                throw new BusinessException(product.getName() + " 的商品数据异常，请联系管理员");
            }
            if (product.getStock() < quantity) {
                throw new BusinessException(product.getName() + " 库存不足");
            }
            if (product.getPerOrderLimit() != null && quantity > product.getPerOrderLimit()) {
                throw new BusinessException(product.getName() + " 每笔订单限购 " + product.getPerOrderLimit() + " 件");
            }
            long purchasedQuantity = purchasedQuantities.getOrDefault(product.getId(), 0L);
            if (product.getCustomerTotalLimit() != null
                    && purchasedQuantity + quantity > product.getCustomerTotalLimit()) {
                long remaining = Math.max(0, product.getCustomerTotalLimit() - purchasedQuantity);
                throw new BusinessException(product.getName() + " 每位客户累计限购 "
                        + product.getCustomerTotalLimit() + " 件，您已购买 " + purchasedQuantity
                        + " 件，本次最多还能购买 " + remaining + " 件");
            }
            totalPointsLong += (long) product.getPointsCost() * quantity;
            if (totalPointsLong > Integer.MAX_VALUE) {
                throw new BusinessException("订单积分总额过大");
            }
            productsById.put(product.getId(), product);
        }
        int totalPoints = (int) totalPointsLong;
        if (request.getExpectedTotalPoints() != null
                && request.getExpectedTotalPoints() != totalPoints) {
            throw new BusinessException("商品积分价已发生变化，请重新确认订单");
        }
        int balanceBefore = pointsService.balanceForUpdate(customerId);
        if (request.getExpectedBalanceBefore() != null
                && request.getExpectedBalanceBefore() != balanceBefore) {
            throw new BusinessException("积分余额已发生变化，请重新确认订单");
        }
        if (balanceBefore < totalPoints) {
            throw new BusinessException("积分不足");
        }
        OrderMain order = new OrderMain();
        order.setOrderNo("PO" + UUID.randomUUID().toString().replace("-", ""));
        order.setCustomerId(customerId);
        order.setCheckoutToken(checkoutToken);
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
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int quantity = entry.getValue();
            int stockBefore = product.getStock();
            product.setStock(stockBefore - quantity);
            productRepository.save(product);
            productTransactionService.customerOrder(product, stockBefore, product.getStock(),
                    saved.getId(), customerId, user.getName());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(saved.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductCoverImage(product.getCoverImageUrl());
            orderItem.setPointsCost(product.getPointsCost());
            orderItem.setQuantity(quantity);
            orderItemRepository.save(orderItem);
        }
        notificationService.createOrderCreated(saved);
        return saved;
    }

    private String normalizeCheckoutToken(String checkoutToken) {
        if (checkoutToken == null || !checkoutToken.matches("^[A-Za-z0-9_-]{16,64}$")) {
            throw new BusinessException("订单提交标识不正确，请返回购物车后重试");
        }
        return checkoutToken;
    }

    public List<OrderDtos.PurchaseAvailability> purchaseAvailability(Long customerId, List<Long> requestedProductIds) {
        List<Long> productIds = requestedProductIds.stream().distinct().sorted().toList();
        if (productIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException("商品信息不正确");
        }
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Long> totals = effectivePurchaseTotals(customerId, productIds);
        return products.stream().map(product -> {
            long purchasedQuantity = totals.getOrDefault(product.getId(), 0L);
            Long remainingTotal = product.getCustomerTotalLimit() == null
                    ? null
                    : Math.max(0L, (long) product.getCustomerTotalLimit() - purchasedQuantity);
            return new OrderDtos.PurchaseAvailability(product.getId(), product.getPerOrderLimit(),
                    product.getCustomerTotalLimit(), purchasedQuantity, remainingTotal);
        }).toList();
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

    public List<OrderItem> ownedOrderItems(Long orderId, Long customerId) {
        ownedOrder(orderId, customerId);
        return orderItems(orderId);
    }

    @Transactional
    public OrderMain ship(Long orderId, AdminDtos.ShipOrderRequest request) {
        OrderMain order = getOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("当前订单不可发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippingCompany(request.getShippingCompany().trim());
        order.setShippingNo(request.getShippingNo().trim());
        order.setShippedAt(LocalDateTime.now());
        return orderMainRepository.save(order);
    }

    @Transactional
    public void confirm(Long orderId, Long customerId) {
        OrderMain order = ownedOrderForUpdate(orderId, customerId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException("当前订单不可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderMainRepository.save(order);
    }

    @Transactional
    public int customerCancel(Long orderId, Long customerId) {
        OrderMain order = ownedOrderForUpdate(orderId, customerId);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("仅待发货订单可以取消");
        }
        return cancelOrder(order, OrderStatus.MANUAL_CANCELLED).getBalanceAfter();
    }

    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        for (OrderMain candidate : orderMainRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_SHIPMENT, deadline)) {
            OrderMain order = getOrderForUpdate(candidate.getId());
            if (order.getStatus() == OrderStatus.PENDING_SHIPMENT && order.getCreatedAt().isBefore(deadline)) {
                cancelOrder(order, OrderStatus.AUTO_CANCELLED);
            }
        }
    }

    @Transactional
    public void autoCompleteShippedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        for (OrderMain candidate : orderMainRepository.findByStatusAndShippedAtBefore(OrderStatus.SHIPPED, deadline)) {
            OrderMain order = getOrderForUpdate(candidate.getId());
            if (order.getStatus() == OrderStatus.SHIPPED && order.getShippedAt() != null && order.getShippedAt().isBefore(deadline)) {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedAt(LocalDateTime.now());
                orderMainRepository.save(order);
            }
        }
    }

    private PointsTransaction cancelOrder(OrderMain order, OrderStatus status) {
        order.setStatus(status);
        order.setCancelledAt(LocalDateTime.now());
        orderMainRepository.save(order);
        TreeMap<Long, Integer> quantities = aggregateOrderItems(orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        List<Product> products = productRepository.findAllByIdForUpdate(new ArrayList<>(quantities.keySet()));
        if (products.size() != quantities.size()) {
            throw new BusinessException("订单商品不存在");
        }
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            int stockBefore = product.getStock();
            int stockAfter;
            try {
                stockAfter = Math.addExact(stockBefore, quantity);
            } catch (ArithmeticException ex) {
                throw new BusinessException("返还库存数值过大");
            }
            product.setStock(stockAfter);
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

    private OrderMain ownedOrderForUpdate(Long id, Long customerId) {
        OrderMain order = getOrderForUpdate(id);
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    private OrderMain getOrder(Long id) {
        return orderMainRepository.findById(id).orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private OrderMain getOrderForUpdate(Long id) {
        return orderMainRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private TreeMap<Long, Integer> aggregateCheckoutItems(List<OrderDtos.CheckoutItem> items) {
        TreeMap<Long, Integer> quantities = new TreeMap<>();
        if (items == null || items.isEmpty() || items.size() > 50) {
            throw new BusinessException("购物车商品数量不正确");
        }
        for (OrderDtos.CheckoutItem item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null
                    || item.getQuantity() < 1 || item.getQuantity() > 999) {
                throw new BusinessException("商品数量必须在 1 到 999 之间");
            }
            try {
                quantities.merge(item.getProductId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new BusinessException("商品数量过大");
            }
            if (quantities.get(item.getProductId()) > 999) {
                throw new BusinessException("单件商品每次最多兑换 999 个");
            }
        }
        return quantities;
    }

    private TreeMap<Long, Integer> aggregateOrderItems(List<OrderItem> items) {
        TreeMap<Long, Integer> quantities = new TreeMap<>();
        for (OrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("订单商品数量异常");
            }
            try {
                quantities.merge(item.getProductId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new BusinessException("订单商品数量异常");
            }
        }
        return quantities;
    }

    private Map<Long, Long> effectivePurchaseTotals(Long customerId, List<Long> productIds) {
        Map<Long, Long> totals = new HashMap<>();
        if (productIds.isEmpty()) return totals;
        for (OrderItemRepository.CustomerProductPurchaseTotal total
                : orderItemRepository.sumCustomerEffectivePurchases(customerId, productIds, EFFECTIVE_PURCHASE_STATUSES)) {
            totals.put(total.getProductId(), total.getPurchasedQuantity());
        }
        return totals;
    }
}
