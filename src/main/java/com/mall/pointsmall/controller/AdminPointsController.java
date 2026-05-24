package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.PointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/points")
public class AdminPointsController {
    private final PointsService pointsService;
    private final AdminPermissionService adminPermissionService;

    public AdminPointsController(PointsService pointsService, AdminPermissionService adminPermissionService) {
        this.pointsService = pointsService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/transactions")
    public ApiResponse<?> transactions(@RequestParam(value = "customerId", required = false) Long customerId) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.POINTS);
        return ApiResponse.ok(pointsService.transactions(customerId));
    }
}
