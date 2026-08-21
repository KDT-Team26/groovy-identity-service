package com.groovy.backend.eventcontract;

import java.time.Instant;
import java.util.UUID;

/**
 * 서비스 간 비동기 이벤트의 공통 봉투. Phase 9(메시지 브로커 도입)에서 실제 Kafka/RabbitMQ
 * 메시지 바디로 이 구조를 그대로 쓴다. payload는 이 모듈의 다른 record 중 하나여야 한다.
 * (Groovy_MSA_Phase2_서비스경계와Contract.md §3-3 스키마를 그대로 타입으로 옮긴 것)
 */
public record EventEnvelope<T>(
	UUID eventId,
	String eventType,
	Instant occurredAt,
	int schemaVersion,
	T payload
) {

	private static final int CURRENT_SCHEMA_VERSION = 1;

	public static <T> EventEnvelope<T> of(String eventType, T payload) {
		return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), CURRENT_SCHEMA_VERSION, payload);
	}
}
