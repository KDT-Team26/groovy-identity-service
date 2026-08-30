package com.groovy.backend.identity.auth;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.RsaPublicJwk;
import lombok.extern.slf4j.Slf4j;

/**
 * MSA 전환(identity-service 추출): groovy(legacy-monolith)의 JwtKeyProvider를 그대로 옮겨왔다 —
 * 이제 identity-service가 실제 발급자다. 비대칭키(RSA)로 서명하고, 공개키만 JWKS
 * (/.well-known/jwks.json)로 공개한다.
 *
 * (#21) 예전엔 이 키를 인스턴스 기동 시 메모리에서 항상 새로 생성했다 — replica가 2개 이상이면
 * pod마다 다른 키를 쓰게 되어, 다른 서비스가 JWKS를 어느 pod에서 받느냐에 따라 서로 발급한
 * 토큰의 서명 검증이 실패하는 문제가 실제로 발생했다(kube-proxy 로드밸런싱 때문에 ~50% 확률로
 * 401). JWT_PRIVATE_KEY_PEM 환경변수(Secret, 모든 replica 공유)가 있으면 그 고정 키를 쓰고,
 * 없으면(로컬 개발 등 단일 인스턴스 상황) 예전처럼 메모리에서 임시 키를 생성한다.
 */
@Slf4j
@Component
public class JwtKeyProvider {

	private final KeyPair keyPair;
	private final RsaPublicJwk publicJwk;

	public JwtKeyProvider(@Value("${JWT_PRIVATE_KEY_PEM:}") String privateKeyPem) {
		if (privateKeyPem == null || privateKeyPem.isBlank()) {
			log.warn("JWT_PRIVATE_KEY_PEM이 설정되지 않아 메모리에서 임시 키를 생성한다 — "
				+ "replica가 2개 이상이면 서로 발급한 토큰을 검증하지 못한다(로컬 개발 전용 동작).");
			this.keyPair = Jwts.SIG.RS256.keyPair().build();
		} else {
			this.keyPair = loadKeyPair(privateKeyPem);
		}
		this.publicJwk = Jwks.builder()
			.key((RSAPublicKey) keyPair.getPublic())
			.idFromThumbprint()
			.build();
	}

	private static KeyPair loadKeyPair(String pem) {
		try {
			String base64 = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
			byte[] decoded = Base64.getDecoder().decode(base64);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
			RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
				new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
			return new KeyPair(publicKey, privateKey);
		} catch (Exception e) {
			throw new IllegalStateException(
				"JWT_PRIVATE_KEY_PEM 파싱 실패 — PKCS8 PEM 형식(-----BEGIN PRIVATE KEY-----)인지 확인",
				e);
		}
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
