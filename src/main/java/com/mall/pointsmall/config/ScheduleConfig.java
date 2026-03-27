package com.mall.pointsmall.config;

import com.mall.pointsmall.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduleConfig {
    private final OrderService orderService;

    public ScheduleConfig(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void autoCancelOrders() {
        orderService.autoCancelExpiredOrders();
    }

    @Scheduled(cron = "0 15 * * * ?")
    public void autoCompleteOrders() {
        orderService.autoCompleteShippedOrders();
    }
}
