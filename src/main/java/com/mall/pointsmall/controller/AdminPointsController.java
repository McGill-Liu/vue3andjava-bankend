package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.PointsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/points")
public class AdminPointsController {
    private final PointsService pointsService;
    private final AdminPermissionService adminPermissionService;

    public AdminPointsController(PointsService pointsService, AdminPermissionService adminPermissionService) {
        this.pointsService = pointsService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/accounts")
    public ApiResponse<?> accounts() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.POINTS);
        return ApiResponse.ok(pointsService.listAccounts());
    }

    @GetMapping("/transactions")
    public ApiResponse<?> transactions(@RequestParam(required = false) Long customerId) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.POINTS);
        return ApiResponse.ok(pointsService.transactions(customerId));
    }

    @PostMapping("/adjustments")
    public ApiResponse<Void> adjust(@Valid @RequestBody AdminDtos.PointsAdjustmentRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.POINTS);
        pointsService.adjust(request);
        return ApiResponse.ok("积分已调整", null);
    }
}
