package com.project.config.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.member.Member;
import com.project.util.JWTUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	private final AuthenticationManager authenticationManager;
	private final ObjectMapper mapper;

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		try {
			Member member = mapper.readValue(request.getInputStream(), Member.class);
			log.info("로그인 시도: username={}", member.getUsername());
			Authentication authToken = new UsernamePasswordAuthenticationToken(member.getUsername(),
					member.getPassword());
			return authenticationManager.authenticate(authToken);
		} catch (IOException e) {
			log.error("로그인 요청 JSON 파싱 오류: {}", e.getMessage());
			throw new RuntimeException("잘못된 로그인 요청 형식입니다.", e);
		}
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		User user = (User) authResult.getPrincipal();
		String username = user.getUsername();
		String accessToken = JWTUtil.getJWT(username);
		response.addHeader(HttpHeaders.AUTHORIZATION, accessToken);
		String refreshToken = JWTUtil.getRefreshJWT(username);
		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
	            .httpOnly(true)
	            .path("/")
	            .maxAge(60 * 60 * 24 * 7)
	            .sameSite("None")
	            .secure(true)
	            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
		response.setStatus(HttpStatus.OK.value());
		log.info("로그인 성공: username={}, Access Token 및 Refresh Token 발급", username);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		log.warn("로그인 실패: {}", failed.getMessage());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("error", "아이디 또는 비밀번호가 맞지 않습니다.");
		response.getWriter().write(mapper.writeValueAsString(responseBody));
	}
}
