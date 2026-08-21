package com.groovy.backend.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.groovy.backend.common.auth.JwtAuthenticationEntryPoint;
import com.groovy.backend.identity.auth.JwtAuthenticationFilter;
import com.groovy.backend.identity.auth.TokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PERMIT_ALL_PATTERNS = {
		"/api/auth/signup",
		"/api/auth/login",
		"/actuator/health",
		"/actuator/prometheus",
		// 다른 서비스가 서명 검증용 공개키를 가져가는 경로. "공개"키라 인증 없이 열려 있어야 한다.
		"/.well-known/jwks.json",
		// MSA 전환(study-service 추출): 스터디 목록 등 비로그인으로도 볼 수 있는 화면이
		// leaderName을 채우려고 부르는 서비스 간 배치 조회라, 호출자가 로그인 상태가 아닐 수 있다.
		"/api/users/names"
	};

	// MSA 전환(Tag 소유권 확정): 전체 태그 목록 조회는 groovy(레거시)에서도 비로그인 사용자에게
	// 열려 있었다(groovy SecurityConfig의 PERMIT_ALL_GET_PATTERNS 참고).
	private static final String[] PERMIT_ALL_GET_PATTERNS = {
		"/api/tags"
	};

	private final TokenProvider tokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
				.requestMatchers(HttpMethod.GET, PERMIT_ALL_GET_PATTERNS).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.anonymous(AbstractHttpConfigurer::disable)
			.addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
