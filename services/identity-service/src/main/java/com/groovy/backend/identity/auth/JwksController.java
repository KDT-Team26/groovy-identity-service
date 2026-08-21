package com.groovy.backend.identity.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.security.Jwk;

import lombok.RequiredArgsConstructor;

/**
 * MSA 전환(identity-service 추출): groovy(legacy-monolith)의 JwksController를 그대로 옮겨왔다.
 * 다른 서비스(notification-service, groovy 등)가 여기서 공개키를 가져가 JWT 서명을 검증한다.
 * 인증 없이 완전히 열려 있어야 한다(SecurityConfig의 permitAll 목록에 반드시 포함).
 *
 * jjwt의 JwkSet/Jwk를 그대로 반환하면 Jackson이 빈 객체로 직렬화해버리는 문제(groovy에서 실제로
 * 겪음)가 있어 순수 LinkedHashMap/List로 한 번 더 감싸서 반환한다.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

	private final JwtKeyProvider jwtKeyProvider;

	@GetMapping("/.well-known/jwks.json")
	public Map<String, Object> jwks() {
		List<Map<String, Object>> keys = jwtKeyProvider.jwkSet().getKeys().stream()
			.map(this::toPlainMap)
			.toList();
		return Map.of("keys", keys);
	}

	private Map<String, Object> toPlainMap(Jwk<?> jwk) {
		return new LinkedHashMap<>(jwk);
	}
}
