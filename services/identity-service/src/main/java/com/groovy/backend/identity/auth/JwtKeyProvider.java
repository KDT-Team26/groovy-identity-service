package com.groovy.backend.identity.auth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.RsaPublicJwk;

/**
 * MSA 전환(identity-service 추출): groovy(legacy-monolith)의 JwtKeyProvider를 그대로 옮겨왔다 —
 * 이제 identity-service가 실제 발급자다. 비대칭키(RSA)로 서명하고, 공개키만 JWKS
 * (/.well-known/jwks.json)로 공개한다.
 *
 * 단순화(문서화된 한계, 이관 전과 동일): 이 키는 인스턴스 기동 시 메모리에서 생성하고 재기동하면
 * 바뀐다. identity-service가 지금 단일 인스턴스로만 운영되기 때문에 가능한 단순화다 — 인스턴스를
 * 2개 이상 띄우는 순간 각자 다른 키를 쓰게 되어 JWKS가 깨진다. 실제 배포라면 키를 파일/시크릿
 * 매니저에서 로드해 모든 인스턴스가 공유해야 한다.
 */
@Component
public class JwtKeyProvider {

	private final KeyPair keyPair;
	private final RsaPublicJwk publicJwk;

	public JwtKeyProvider() {
		this.keyPair = Jwts.SIG.RS256.keyPair().build();
		this.publicJwk = Jwks.builder()
			.key((RSAPublicKey) keyPair.getPublic())
			.idFromThumbprint()
			.build();
	}

	public PrivateKey privateKey() {
		return keyPair.getPrivate();
	}

	public RSAPublicKey publicKey() {
		return (RSAPublicKey) keyPair.getPublic();
	}

	public String keyId() {
		return publicJwk.getId();
	}

	// /.well-known/jwks.json이 그대로 반환하는 공개키 집합.
	public JwkSet jwkSet() {
		return Jwks.set().add(publicJwk).build();
	}
}
