package com.groovy.backend.identity.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groovy.backend.identity.Tag;
import com.groovy.backend.identity.User;
import com.groovy.backend.identity.UserTag;
import com.groovy.backend.identity.dto.TagResponse;
import com.groovy.backend.identity.repository.TagRepository;
import com.groovy.backend.identity.repository.UserTagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MSA 전환(Tag 소유권 확정): groovy(레거시)의 domain/tag/service/TagService.java를 그대로
 * 옮겨왔다. StudyTag 관련 메서드는 이미 2단계(study-service 추출)에서 그쪽으로 옮겨갔으므로
 * 여기는 태그 마스터 목록 조회 + UserTag(선호 태그)만 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

	private final TagRepository tagRepository;
	private final UserTagRepository userTagRepository;
	private final UserService userService;

	public List<TagResponse> getTags() {
		return tagRepository.findAll().stream()
			.map(TagResponse::from)
			.toList();
	}

	public List<TagResponse> getUserTags(String email) {
		User user = getUser(email);
		return userTagRepository.findByUserId(user.getId()).stream()
			.map(userTag -> TagResponse.from(userTag.getTag()))
			.toList();
	}

	// 겹치는 태그를 delete-all 후 insert-all로 처리하면, 같은 (user_id, tag_id)에 대해
	// 삭제 대상 row가 아직 남아있는 상태로 새 row를 insert하려다 uk_user_tag 유니크 제약을
	// 위반할 수 있다(Hibernate가 flush 시 INSERT를 DELETE보다 먼저 실행하기 때문).
	// 그래서 기존/신규 목록을 비교해 실제로 빠진 것만 삭제, 새로 추가된 것만 삽입한다.
	@Transactional
	public void updateUserTags(String email, List<Long> tagIds) {
		User user = getUser(email);
		List<Tag> tags = resolveTags(tagIds);
		Set<Long> newTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());

		List<UserTag> existingUserTags = userTagRepository.findByUserId(user.getId());
		Set<Long> existingTagIds = existingUserTags.stream()
			.map(userTag -> userTag.getTag().getId())
			.collect(Collectors.toSet());

		List<UserTag> toRemove = existingUserTags.stream()
			.filter(userTag -> !newTagIds.contains(userTag.getTag().getId()))
			.toList();
		userTagRepository.deleteAll(toRemove);

		List<UserTag> toAdd = tags.stream()
			.filter(tag -> !existingTagIds.contains(tag.getId()))
			.map(tag -> UserTag.builder().userId(user.getId()).tag(tag).build())
			.toList();
		userTagRepository.saveAll(toAdd);
		log.info("유저 관심 태그 변경: email={}, tagIds={}", email, tagIds);
	}

	private List<Tag> resolveTags(List<Long> tagIds) {
		if (tagIds.isEmpty()) {
			return List.of();
		}

		List<Tag> tags = tagRepository.findAllById(tagIds);
		if (tags.size() != new HashSet<>(tagIds).size()) {
			log.warn("존재하지 않는 태그 포함: tagIds={}", tagIds);
			throw new IllegalArgumentException("존재하지 않는 태그가 포함되어 있습니다.");
		}

		return tags;
	}

	private User getUser(String email) {
		return userService.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("존재하지 않는 유저: email={}", email);
				return new IllegalArgumentException("존재하지 않는 유저입니다.");
			});
	}
}
