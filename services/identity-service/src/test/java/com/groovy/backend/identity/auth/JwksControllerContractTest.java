package com.groovy.backend.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Key;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;

/**
 * MSA 전환(identity-service 추출): groovy(레거시)의 JwksControllerContractTest를 그대로
 * 옮겨왔다 — 이제 identity-service가 Provider(발급자)다. Consumer 쪽 계약 테스트는
 * notification-service의 JwksKeyLocatorContractTest 참고.
 *
 * Spring Cloud Contract 대신, 실제 컨슈머가 쓰는 라이브러리(jjwt의 Jwks.setParser())로
 * JwksController의 실제 응답을 직접 파싱해서 "진짜로 소비 가능한지" 검증한다.
 */
class JwksControllerContractTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void jwks_응답을_jjwt_파서로_그대로_소비할_수_있다() throws Exception {
		JwtKeyProvider keyProvider = new JwtKeyProvider();
		JwksController controller = new JwksController(keyProvider);

		Map<String, Object> body = controller.jwks();
		String json = objectMapper.writeValueAsString(body);

		JwkSet jwkSet = Jwks.setParser().build().parse(json);

		assertThat(jwkSet.getKeys()).hasSize(1);
		Jwk<?> jwk = jwkSet.getKeys().iterator().next();
		assertThat(jwk.getId()).isEqualTo(keyProvider.keyId());
		assertThat(jwk.get("kty")).isEqualTo("RSA");

		Key key = jwk.toKey();
		assertThat(key).isEqualTo(keyProvider.publicKey());
	}
}
