package com.groovy.backend.common.auth;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.groovy.backend.common.response.ApiResponse;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 5개 서비스에 복붙되어 있던 클래스를 web-common으로 통합했다(WebCommonAutoConfiguration이
 * 빈으로 등록). 인증 필요 여부만 판단하고, 실제 토큰 검증 방식(발급자 identity-service vs
 * 검증자 나머지 4개 서비스)과는 무관해서 5개 서비스 전체가 공유할 수 있다.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Void> body = ApiResponse.of("FAIL", "인증이 필요하거나 유효하지 않은 토큰입니다.");
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
