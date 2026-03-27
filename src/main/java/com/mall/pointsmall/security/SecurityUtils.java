package com.mall.pointsmall.security;

import com.mall.pointsmall.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static SecurityUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser user) {
            return user;
        }
        throw new BusinessException("未登录");
    }
}
