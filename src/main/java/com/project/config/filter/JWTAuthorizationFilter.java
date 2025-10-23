package com.project.config.filter;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.domain.member.Member;
import com.project.persistence.MemberRepository;
import com.project.util.JWTUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JWTAuthorizationFilter extends OncePerRequestFilter {
	private final MemberRepository memberRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String srcToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (srcToken == null || !srcToken.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String jwtToken = srcToken.replace("Bearer ", "");

		String username = JWTUtil.getClaim(jwtToken);
		Optional<Member> opt = memberRepository.findById(username);
		if (!opt.isPresent()) {
			log.warn("JWT 토큰은 유효하나 사용자를 찾을 수 없음: username={}", username);
			filterChain.doFilter(request, response);
			return;
		}
		Member findmember = opt.get();
		if (!findmember.isEnabled()) {
			log.warn("계정이 비활성화된 사용자의 접근 시도: username={}", username);
		    filterChain.doFilter(request, response);
		    return;
		}
		User user = new User(findmember.getUsername(), findmember.getPassword(),
				AuthorityUtils.createAuthorityList(findmember.getRole().toString()));
		Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
		log.info("JWT 인증 성공, SecurityContext에 사용자 등록: username={}", username);
		
		filterChain.doFilter(request, response);
	}
}
