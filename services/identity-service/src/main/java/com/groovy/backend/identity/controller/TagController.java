package com.groovy.backend.identity.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groovy.backend.common.response.ApiResponse;
import com.groovy.backend.identity.dto.TagResponse;
import com.groovy.backend.identity.dto.UserTagUpdateRequest;
import com.groovy.backend.identity.service.TagService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * MSA 전환(Tag 소유권 확정): groovy(레거시)의 domain/tag/controller/TagController.java를 그대로
 * 옮겨왔다. URL(/api/tags, /api/tags/me)은 프론트엔드 호환을 위해 바뀌지 않았다 — API Gateway가
 * 이 경로를 identity-service로 라우팅한다. study-service의 TagPreferenceClient가 "/api/tags/me"를
 * 소비한다(스터디 매칭 시 로그인 유저의 선호 태그로 대체).
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

	private final TagService tagService;

	@GetMapping
	public ApiResponse<List<TagResponse>> getTags() {
		return ApiResponse.of("SUCCESS", "전체 태그 목록 조회에 성공했습니다.", tagService.getTags());
	}

	@GetMapping("/me")
	public ApiResponse<List<TagResponse>> getMyTags(@AuthenticationPrincipal String email) {
		return ApiResponse.of("SUCCESS", "내 선호 태그 조회에 성공했습니다.", tagService.getUserTags(email));
	}

	@PutMapping("/me")
	public ApiResponse<Void> updateMyTags(
		@AuthenticationPrincipal String email,
		@Valid @RequestBody UserTagUpdateRequest request
	) {
		tagService.updateUserTags(email, request.tagIds());
		return ApiResponse.of("SUCCESS", "선호 태그가 저장되었습니다.");
	}
}
