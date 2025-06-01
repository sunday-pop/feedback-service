package org.example.feedbackservice.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.feedbackservice.global.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletRequest request;

    public String getCurrentUserId() {
        // 1. SecurityContext에서 먼저 시도
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            if (StringUtils.hasText(token)) {
                return jwtTokenProvider.resolveUserId(token);
            }
        }

        // 2. 실패 시 직접 요청에서 토큰 추출
        String token = resolveToken(request);
        if (token == null) {
            throw new IllegalStateException("No authentication token found");
        }
        return jwtTokenProvider.resolveUserId(token);
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더에서 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 쿠키에서 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
