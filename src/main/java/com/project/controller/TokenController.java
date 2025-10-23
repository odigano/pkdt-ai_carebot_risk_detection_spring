package com.project.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.util.JWTUtil;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TokenController {
    @PostMapping("/api/refresh")
    public ResponseEntity<String> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
    	log.info("Access Token 갱신 요청 수신");
    	if (refreshToken == null) {
    		log.warn("Refresh Token이 쿠키에 존재하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 없음");
    	}
        String username = JWTUtil.getRefreshClaim(refreshToken);
        log.info("Refresh Token 검증 성공: username={}", username);
        String newAccessToken = JWTUtil.getJWT(username);
        String newRefreshToken = JWTUtil.getRefreshJWT(username);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(60 * 60 * 24 * 7)
                .sameSite("None")
                .secure(true)
                .build();
        log.info("새로운 Access Token 및 Refresh Token 발급 완료: username={}", username);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, newAccessToken)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(null);
    }
}