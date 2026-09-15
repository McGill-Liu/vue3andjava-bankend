package com.mall.pointsmall.dto;

import lombok.Data;

import java.time.LocalDateTime;

public class OperationRecordDtos {
    @Data
    public static class Response {
        private Long id;
        private Long operatorId;
        private String operatorName;
        private String action;
        private String targetType;
        private Long targetId;
        private String targetName;
        private boolean success;
        private String message;
        private String requestIp;
        private LocalDateTime createdAt;
    }
}
