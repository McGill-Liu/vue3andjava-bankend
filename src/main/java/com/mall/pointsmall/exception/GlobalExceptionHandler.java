package com.mall.pointsmall.exception;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.service.OperationRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final OperationRecordService operationRecordService;
    private final HttpServletRequest request;

    public GlobalExceptionHandler(OperationRecordService operationRecordService, HttpServletRequest request) {
        this.operationRecordService = operationRecordService;
        this.request = request;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        recordFailure(ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        recordFailure(message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("接口不存在"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail("上传图片不能超过 10MB"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        recordFailure("提交的数据不符合要求");
        return ResponseEntity.badRequest().body(ApiResponse.fail("提交的数据不符合要求，请检查后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        recordFailure("服务器处理异常");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("服务器内部错误，请稍后重试"));
    }

    private void recordFailure(String message) {
        AuditTarget target = auditTarget(request.getMethod(), request.getRequestURI());
        if (target == null) {
            return;
        }
        try {
            operationRecordService.recordFailure(target.action(), target.targetType(), target.targetId(), message);
        } catch (Exception ignored) {
            // Auditing must not replace the original business error.
        }
    }

    private AuditTarget auditTarget(String method, String uri) {
        if ("POST".equals(method) && "/api/admin/users".equals(uri)) return new AuditTarget("新增客户", "客户", null);
        if ("POST".equals(method) && uri.matches("/api/admin/users/\\d+/reset-password")) return new AuditTarget("重置客户密码", "客户", pathId(uri));
        if ("PUT".equals(method) && uri.matches("/api/admin/users/\\d+")) return new AuditTarget("修改客户资料", "客户", pathId(uri));
        if ("PUT".equals(method) && uri.matches("/api/admin/users/\\d+/points-balance")) return new AuditTarget("调整客户积分", "客户", pathId(uri));
        if ("POST".equals(method) && "/api/admin/admin-users".equals(uri)) return new AuditTarget("新增管理员", "管理员", null);
        if ("PUT".equals(method) && uri.matches("/api/admin/admin-users/\\d+")) return new AuditTarget("修改管理员权限", "管理员", pathId(uri));
        if ("PATCH".equals(method) && uri.matches("/api/admin/admin-users/\\d+/status")) return new AuditTarget("修改管理员状态", "管理员", pathId(uri));
        if ("PATCH".equals(method) && uri.matches("/api/admin/admin-users/\\d+/reset-password")) return new AuditTarget("重置管理员密码", "管理员", pathId(uri));
        if ("POST".equals(method) && "/api/admin/product-categories".equals(uri)) return new AuditTarget("新增商品分类", "商品分类", null);
        if ("PUT".equals(method) && uri.matches("/api/admin/product-categories/\\d+")) return new AuditTarget("修改商品分类", "商品分类", pathId(uri));
        if ("POST".equals(method) && "/api/admin/products".equals(uri)) return new AuditTarget("新增或修改商品", "商品", null);
        if ("POST".equals(method) && uri.matches("/api/admin/products/\\d+/stock")) return new AuditTarget("调整商品库存", "商品", pathId(uri));
        if ("POST".equals(method) && uri.matches("/api/admin/products/\\d+/status")) return new AuditTarget("修改商品状态", "商品", pathId(uri));
        if ("POST".equals(method) && uri.matches("/api/admin/orders/\\d+/ship")) return new AuditTarget("订单发货", "订单", pathId(uri));
        if ("POST".equals(method) && uri.matches("/api/admin/notifications/\\d+/process")) return new AuditTarget("处理待办事项", "待办事项", pathId(uri));
        if ("POST".equals(method) && "/api/auth/change-password".equals(uri)) return new AuditTarget("修改本人密码", "管理员", null);
        return null;
    }

    private Long pathId(String uri) {
        String[] parts = uri.split("/");
        for (int index = 0; index < parts.length - 1; index++) {
            if ("users".equals(parts[index]) || "admin-users".equals(parts[index]) || "product-categories".equals(parts[index])
                    || "products".equals(parts[index]) || "orders".equals(parts[index]) || "notifications".equals(parts[index])) {
                return Long.parseLong(parts[index + 1]);
            }
        }
        return null;
    }

    private record AuditTarget(String action, String targetType, Long targetId) {
    }
}
