package com.groovy.backend.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// MSA 전환: groovy(레거시)의 domain/user/User.java를 그대로 옮겨왔다. identity-service가
// 이 Aggregate의 발급자/소유자가 된다(groovy에는 읽기 전용 축소 버전이 남아있다 —
// groovy/.../domain/user/service/UserService.java 주석 참고).
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	// 소셜 로그인 유저는 비밀번호가 없을 수 있어 nullable 허용
	@Column
	private String password;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProviderType providerType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoleType roleType;

	@Builder
	public User(String email, String password, String name, ProviderType providerType, RoleType roleType) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.providerType = providerType;
		this.roleType = roleType;
	}
}
