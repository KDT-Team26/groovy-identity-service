package com.groovy.backend.identity.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groovy.backend.common.response.ApiResponse;
import com.groovy.backend.identity.dto.UserResponse;
import com.groovy.backend.identity.service.UserService;

import lombok.RequiredArgsConstructor;

// MSA 전환: "내가 만든 스터디"/"내 신청 내역"(/me/studies, /me/applications)은 Study 데이터라
// 여기 옮겨오지 않았다 — study-service 추출 때 그쪽으로 옮겨갔다
// (backend/services/study-service/.../controller/UserStudyController.java 참고).
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal String email) {
		return ApiResponse.of("SUCCESS", "내 정보 조회에 성공했습니다.", userService.getMyInfo(email));
	}

	// MSA 전환(study-service 추출): 게이트웨이를 거치지 않고 서비스 간 직접 호출로만 쓰는
	// 배치 조회다(study-service의 UserServiceClient, groovy의 StudyServiceClient가 소비).
	// 공개 API가 아니므로 API Gateway에 이 경로에 대한 라우트를 추가하지 않는다.
	@GetMapping("/names")
	public ApiResponse<Map<Long, String>> getNames(@RequestParam List<Long> ids) {
		return ApiResponse.of("SUCCESS", "유저 이름 조회에 성공했습니다.", userService.findNamesByIds(ids));
	}
}
