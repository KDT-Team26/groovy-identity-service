package com.groovy.backend.identity.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record UserTagUpdateRequest(
	@NotNull(message = "선호 태그 목록은 필수입니다.")
	List<Long> tagIds
) {
}
