package com.groovy.backend.identity;

import com.groovy.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User(N:1) - Tag(N:1) 매핑 중개 엔티티. {@code @ManyToMany} 대신 명시적으로 관계를 풀어
 * 유저의 선호 태그를 독립된 로우로 관리한다.
 *
 * MSA 전환(Tag 소유권 확정): groovy(레거시)의 domain/tag/UserTag.java를 그대로 옮겨왔다. User가
 * 이 서비스 안에 같이 있지만, userId는 groovy 시절 그대로 Long 필드로 유지한다(엔티티 참조로
 * 되돌릴 이유가 없다 — 이미 이 필드 기준으로 리포지토리/서비스 로직이 동작하고 있었다).
 */
@Entity
@Table(
	name = "user_tags",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_user_tag",
		columnNames = {"user_id", "tag_id"}
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTag extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag_id", nullable = false)
	private Tag tag;

	@Builder
	public UserTag(Long userId, Tag tag) {
		this.userId = userId;
		this.tag = tag;
	}
}
