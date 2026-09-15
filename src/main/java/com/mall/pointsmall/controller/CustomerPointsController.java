package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.security.SecurityUser;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.PointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class CustomerPointsController {
    private final PointsService pointsService;

    public CustomerPointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @GetMapping("/balance")
    public ApiResponse<Integer> balance() {
        SecurityUser user = SecurityUtils.currentUser();
        if (user.getRole() != RoleType.CUSTOMER) {
            throw new BusinessException("仅客户账号可查看积分余额");
        }
        return ApiResponse.ok(pointsService.balanceOf(user.getId()));
    }
}
