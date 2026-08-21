package com.groovy.backend.identity.auth;

import java.util.Date;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.groovy.backend.identity.RoleType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MSA 전환(identity-service 추출): groovy(legacy-monolith)의 TokenProvider를 그대로 옮겨왔다.
 * identity-service가 실제 발급자다 — 개인키로 서명은 여기서만 하고, 다른 서비스는 공개키(JWKS)로
 * 검증만 한다(groovy는 이제 JwksKeyLocator로 이 서비스의 JWKS를 참조한다 — groovy/.../global/
 * auth/jwt/TokenProvider.java 참고).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

	private static final String USER_ID_CLAIM_KEY = "uid";
	private static final String ROLE_CLAIM_KEY = "role";
	private static final long ACCESS_TOKEN_EXPIRE_TIME_MILLIS = 1000L * 60 * 60; // 1시간

	private final JwtKeyProvider jwtKeyProvider;

	public String createToken(String email, Long userId, RoleType role) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME_MILLIS);

		return Jwts.builder()
			.header().keyId(jwtKeyProvider.keyId()).and()
			.subject(email)
			.claim(USER_ID_CLAIM_KEY, userId)
			.claim(ROLE_CLAIM_KEY, role.name())
			.issuedAt(now)
			.expiration(expiry)
			.signWith(jwtKeyProvider.privateKey(), Jwts.SIG.RS256)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(jwtKeyProvider.publicKey()).build().parseSignedClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("토큰 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			return false;
		}
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		String email = claims.getSubject();
		String role = claims.get(ROLE_CLAIM_KEY, String.class);

		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

		return new UsernamePasswordAuthenticationToken(email, null, authorities);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(jwtKeyProvider.publicKey()).build().parseSignedClaims(token).getPayload();
	}
}
