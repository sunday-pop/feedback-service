package org.example.feedbackservice.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    public SecretKey getSecretKey() {
        byte[] keyBytes = secretKey.getBytes();
        log.debug("🛡️ JWT 시크릿 키 길이 (bytes): {}", keyBytes.length);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String resolveUserId(String token) {
        log.info(token);
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 1. UUID(userId)가 클레임에 있는지 우선 확인
        if (claims.containsKey("userId")) {
            return claims.get("userId", String.class); // UUID 반환
        }

        // 2. 없으면 email을 식별자로 사용 (임시)
        String email = claims.getSubject();
        log.warn("JWT에 userId가 없습니다. email로 대체 사용: {}", email);
        return email; // 또는 email → UUID 조회 로직 추가
    }


    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles");
    }

    public Authentication getAuthentication(String token) {
        UserDetails user = new User(getUsername(token), "",
                getRoles(token).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());

        return new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
    }
}
