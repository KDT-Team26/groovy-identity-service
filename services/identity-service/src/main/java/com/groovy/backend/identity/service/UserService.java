package com.groovy.backend.identity.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groovy.backend.identity.ProviderType;
import com.groovy.backend.identity.RoleType;
import com.groovy.backend.identity.User;
import com.groovy.backend.identity.auth.TokenProvider;
import com.groovy.backend.identity.dto.LoginRequest;
import com.groovy.backend.identity.dto.LoginResponse;
import com.groovy.backend.identity.dto.SignupRequest;
import com.groovy.backend.identity.dto.UserResponse;
import com.groovy.backend.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// MSA 전환(identity-service 추출): groovy(레거시)의 UserService에서 인증/발급 부분(signup,
// login, getMyInfo)을 그대로 옮겨왔다. groovy에는 다른 도메인이 쓰는 읽기 전용 축소 버전만 남는다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenProvider tokenProvider;

	@Transactional
	public UserResponse signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			log.warn("회원가입 실패(이메일 중복): email={}", request.email());
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}

		User user = User.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.name(request.name())
			.providerType(ProviderType.LOCAL)
			.roleType(RoleType.USER)
			.build();

		User saved = userRepository.save(user);
		log.info("회원가입 성공: email={}, userId={}", saved.getEmail(), saved.getId());

		return UserResponse.from(saved);
	}

	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> {
				log.warn("로그인 실패(존재하지 않는 이메일): email={}", request.email());
				return new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
			});

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			log.warn("로그인 실패(비밀번호 불일치): email={}", request.email());
			throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
		}

		String accessToken = tokenProvider.createToken(user.getEmail(), user.getId(), user.getRoleType());
		log.info("로그인 성공: email={}, userId={}", user.getEmail(), user.getId());
		return LoginResponse.of(accessToken);
	}

	public UserResponse getMyInfo(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("존재하지 않는 유저: email={}", email);
				return new IllegalArgumentException("존재하지 않는 유저입니다.");
			});

		return UserResponse.from(user);
	}

	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public Optional<User> findById(Long userId) {
		return userRepository.findById(userId);
	}

	// MSA 전환(study-service 추출): study-service/groovy(레거시)가 leaderName/authorName 등을
	// 채우기 위해 쓰는 서비스 간 배치 조회. UserRepository를 직접 노출하지 않고 이 메서드를
	// 거치도록 강제한다(groovy UserService.findNamesByIds와 동일한 이유).
	public Map<Long, String> findNamesByIds(List<Long> userIds) {
		return userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
	}
}
