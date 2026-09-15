package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.dto.OrderDtos;
import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AddressService;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.OrderService;
import com.mall.pointsmall.service.OperationRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;
    private final AddressService addressService;
    private final AdminPermissionService adminPermissionService;
    private final OperationRecordService operationRecordService;

    public OrderController(OrderService orderService,
                           AddressService addressService,
                           AdminPermissionService adminPermissionService,
                           OperationRecordService operationRecordService) {
        this.orderService = orderService;
        this.addressService = addressService;
        this.adminPermissionService = adminPermissionService;
        this.operationRecordService = operationRecordService;
    }

    @GetMapping("/addresses")
    public ApiResponse<?> addresses() {
        return ApiResponse.ok(addressService.list(SecurityUtils.currentUser().getId()));
    }

    @PostMapping("/addresses")
    public ApiResponse<?> createAddress(@Valid @RequestBody OrderDtos.AddressRequest request) {
        return ApiResponse.ok(addressService.create(SecurityUtils.currentUser().getId(), request));
    }

    @PutMapping("/addresses/{id}")
    public ApiResponse<?> updateAddress(@PathVariable Long id, @Valid @RequestBody OrderDtos.AddressRequest request) {
        return ApiResponse.ok(addressService.update(id, SecurityUtils.currentUser().getId(), request));
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        addressService.delete(id, SecurityUtils.currentUser().getId());
        return ApiResponse.ok("地址已删除", null);
    }

    @PostMapping("/orders/checkout")
    public ApiResponse<?> checkout(@Valid @RequestBody OrderDtos.CheckoutRequest request) {
        return ApiResponse.ok(orderService.checkout(SecurityUtils.currentUser().getId(), request));
    }

    @PostMapping("/orders/purchase-availability")
    public ApiResponse<?> purchaseAvailability(@Valid @RequestBody OrderDtos.PurchaseAvailabilityRequest request) {
        return ApiResponse.ok(orderService.purchaseAvailability(SecurityUtils.currentUser().getId(), request.getProductIds()));
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(orderService.userOrders(SecurityUtils.currentUser().getId()));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<?> order(@PathVariable Long id) {
        return ApiResponse.ok(orderService.ownedOrderItems(id, SecurityUtils.currentUser().getId()));
    }

    @PostMapping("/orders/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id, SecurityUtils.currentUser().getId());
        return ApiResponse.ok("订单已完成", null);
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<Integer> customerCancel(@PathVariable Long id) {
        int balance = orderService.customerCancel(id, SecurityUtils.currentUser().getId());
        return ApiResponse.ok("订单已取消，积分已返还", balance);
    }

    @GetMapping("/admin/orders")
    public ApiResponse<?> adminOrders() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.ORDERS);
        return ApiResponse.ok(orderService.adminOrders());
    }

    @GetMapping("/admin/orders/{id}")
    public ApiResponse<?> adminOrder(@PathVariable Long id) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.ORDERS);
        return ApiResponse.ok(orderService.orderItems(id));
    }

    @PostMapping("/admin/orders/{id}/ship")
    @Transactional
    public ApiResponse<Void> ship(@PathVariable Long id, @Valid @RequestBody AdminDtos.ShipOrderRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.ORDERS);
        OrderMain order = orderService.ship(id, request);
        operationRecordService.recordSuccess("订单发货", "订单", order.getId(), order.getOrderNo(),
                "物流公司：" + request.getShippingCompany() + "，物流单号：" + request.getShippingNo());
        return ApiResponse.ok("发货成功", null);
    }

}
