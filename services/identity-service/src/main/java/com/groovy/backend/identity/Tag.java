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

/**
 * MSA 전환(Tag 소유권 확정): Tag 마스터 데이터는 identity-service가 소유한다(원래 groovy의
 * domain/tag/Tag.java). study-service는 StudyTag 매칭을 위해 이 테이블의 사본을 자기 스키마에도
 * 갖고 있다(알려진 한계 — docs/Groovy_MSA_도메인경계_재검토.md, 계획 문서 참고).
 */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TagCategory category;

	@Builder
	public Tag(String name, TagCategory category) {
		this.name = name;
		this.category = category;
	}
}
