package com.mall.pointsmall.security;

import com.mall.pointsmall.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SecurityUser {
    private Long id;
    private String name;
    private String phone;
    private RoleType role;
    private Map<String, String> permissions;
}
