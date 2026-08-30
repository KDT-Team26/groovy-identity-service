package com.groovy.backend.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.groovy.backend.observability.TracingConfig;

/**
 * MSA 전환(Tag 소유권 확정): UserTag(BaseTimeEntity 상속, createdAt/updatedAt 자동 채움)를
 * identity-service로 이관하면서 JPA Auditing이 처음 필요해졌다(study-service의 선례와 동일).
 *
 * 공통 코드 분리(groovy-common) 후: observability 모듈의 TracingConfig(@Configuration)가
 * 이 서비스 base 패키지 밖이라 @Import 로 가져온다. (identity 는 발급자라 outbox/security-common 미사용)
 */
@SpringBootApplication
@EnableJpaAuditing
@Import(TracingConfig.class)
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}
}
