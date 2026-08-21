package com.groovy.backend.eventcontract.identity;

/**
 * 초안(§3-2) — identity-service 발행 예정, 현재 소비자 없음. 회원 탈퇴 기능 자체가 아직 없어
 * 발행 코드도 없다. 탈퇴 기능을 구현하는 시점에 study/content/calendar/notification 4개
 * 서비스에 흩어진 userId FK 정리 로직과 함께 발행 코드를 추가한다.
 */
public record UserDeletedPayload(
	Long userId,
	String email
) {
}
