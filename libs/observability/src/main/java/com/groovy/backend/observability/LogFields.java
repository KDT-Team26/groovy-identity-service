package com.groovy.backend.observability;

/**
 * 서비스 전반에서 구조화 로그(MDC)에 공통으로 쓰는 필드명. Phase 12(분산 Observability)에서
 * traceId를 서비스 경계 너머로 전파할 때 이 키로 통일해서 Loki 검색이 가능하게 한다.
 */
public final class LogFields {

	public static final String TRACE_ID = "traceId";
	public static final String SERVICE_NAME = "service";

	private LogFields() {
	}
}
