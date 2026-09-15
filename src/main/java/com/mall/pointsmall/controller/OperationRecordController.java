package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.OperationRecordDtos;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.OperationRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/operation-records")
public class OperationRecordController {
    private final OperationRecordService operationRecordService;
    private final AdminPermissionService adminPermissionService;

    public OperationRecordController(OperationRecordService operationRecordService,
                                     AdminPermissionService adminPermissionService) {
        this.operationRecordService = operationRecordService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping
    public ApiResponse<Page<OperationRecordDtos.Response>> list(
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.OPERATION_RECORDS);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = switch (sortBy) {
            case "operatorName", "action", "success" -> sortBy;
            default -> "createdAt";
        };
        Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ApiResponse.ok(operationRecordService.search(operatorName, action, targetName, success, startAt, endAt,
                PageRequest.of(safePage, safeSize, Sort.by(safeDirection, safeSortBy).and(Sort.by(Sort.Direction.DESC, "id")))));
    }
}
