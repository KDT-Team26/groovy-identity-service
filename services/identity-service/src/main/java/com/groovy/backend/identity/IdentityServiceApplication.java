package com.groovy.backend.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * MSA 전환(Tag 소유권 확정): UserTag(BaseTimeEntity 상속, createdAt/updatedAt 자동 채움)를
 * identity-service로 이관하면서 JPA Auditing이 처음 필요해졌다(study-service의 선례와 동일).
 */
@SpringBootApplication
@EnableJpaAuditing
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}
}
