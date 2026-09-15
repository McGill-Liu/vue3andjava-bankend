package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.OperationRecordDtos;
import com.mall.pointsmall.entity.OperationRecord;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.repository.OperationRecordRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationRecordService {
    private final OperationRecordRepository operationRecordRepository;
    private final HttpServletRequest request;

    public OperationRecordService(OperationRecordRepository operationRecordRepository, HttpServletRequest request) {
        this.operationRecordRepository = operationRecordRepository;
        this.request = request;
    }

    @Transactional
    public void recordSuccess(String action, String targetType, Long targetId, String targetName, String message) {
        save(currentAdmin(), action, targetType, targetId, targetName, true, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String targetType, Long targetId, String message) {
        SecurityUser actor = currentAdmin();
        if (actor != null) {
            save(actor, action, targetType, targetId, null, false, message);
        }
    }

    public Page<OperationRecordDtos.Response> search(String operatorName, String action, String targetName,
                                                      Boolean success, java.time.LocalDateTime startAt,
                                                      java.time.LocalDateTime endAt, Pageable pageable) {
        return operationRecordRepository.search(normalize(operatorName), normalize(action), normalize(targetName), success,
                startAt, endAt, pageable).map(this::response);
    }

    private void save(SecurityUser actor, String action, String targetType, Long targetId, String targetName,
                      boolean success, String message) {
        OperationRecord record = new OperationRecord();
        record.setOperatorId(actor == null ? null : actor.getId());
        record.setOperatorName(actor == null ? "未知账号" : actor.getName());
        record.setAction(action);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setSuccess(success);
        record.setMessage(sanitize(message));
        record.setRequestIp(request.getRemoteAddr());
        operationRecordRepository.save(record);
    }

    private SecurityUser currentAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser user && user.getRole() != RoleType.CUSTOMER) {
            return user;
        }
        return null;
    }

    private OperationRecordDtos.Response response(OperationRecord record) {
        OperationRecordDtos.Response result = new OperationRecordDtos.Response();
        result.setId(record.getId());
        result.setOperatorId(record.getOperatorId());
        result.setOperatorName(record.getOperatorName());
        result.setAction(record.getAction());
        result.setTargetType(record.getTargetType());
        result.setTargetId(record.getTargetId());
        result.setTargetName(record.getTargetName());
        result.setSuccess(record.isSuccess());
        result.setMessage(record.getMessage());
        result.setRequestIp(record.getRequestIp());
        result.setCreatedAt(record.getCreatedAt());
        return result;
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String value = message.replaceAll("(?i)(password|密码|token|authorization)\\s*[:=]\\s*[^,\\s]+", "$1=***");
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
