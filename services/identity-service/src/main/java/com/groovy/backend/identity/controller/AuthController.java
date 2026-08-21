package com.groovy.backend.identity.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groovy.backend.common.response.ApiResponse;
import com.groovy.backend.identity.dto.LoginRequest;
import com.groovy.backend.identity.dto.LoginResponse;
import com.groovy.backend.identity.dto.SignupRequest;
import com.groovy.backend.identity.dto.UserResponse;
import com.groovy.backend.identity.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	@PostMapping("/signup")
	public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.of("SUCCESS", "회원가입이 완료되었습니다.", userService.signup(request));
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse response = userService.login(request);
		return ApiResponse.of("SUCCESS", "로그인에 성공했습니다.", response);
	}

	// 인증은 무상태 JWT로 처리되어 서버에 무효화할 세션/토큰이 없으므로,
	// SecurityConfig의 기본 인증 요구사항으로 유효한 토큰 보유만 확인하고 실제 폐기는 클라이언트가 토큰을 버리는 것으로 완료된다.
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		return ApiResponse.of("SUCCESS", "로그아웃되었습니다.");
	}
}
