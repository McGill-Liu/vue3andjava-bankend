package com.mall.pointsmall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mall.pointsmall.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class WeChatMiniProgramService {
    private final boolean enabled;
    private final String appId;
    private final String appSecret;
    private final RestClient restClient = RestClient.create("https://api.weixin.qq.com");

    public WeChatMiniProgramService(@Value("${app.wechat.open-id-login-enabled:false}") boolean enabled,
                                    @Value("${app.wechat.app-id:}") String appId,
                                    @Value("${app.wechat.app-secret:}") String appSecret) {
        this.enabled = enabled;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<String> tryExchangeCode(String code) {
        if (!enabled || code == null || code.isBlank()) {
            return Optional.empty();
        }
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new BusinessException("微信自动登录尚未完成服务器配置");
        }
        try {
            JsonNode result = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/sns/jscode2session")
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (result != null && result.hasNonNull("openid")) {
                return Optional.of(result.get("openid").asText());
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public String exchangeCode(String code) {
        if (!enabled) {
            throw new BusinessException("微信自动登录尚未启用");
        }
        return tryExchangeCode(code).orElseThrow(() -> new BusinessException("微信登录凭证无效或已过期，请使用密码登录"));
    }
}
