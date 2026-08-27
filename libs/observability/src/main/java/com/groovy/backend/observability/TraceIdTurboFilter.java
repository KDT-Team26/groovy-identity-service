package com.groovy.backend.observability;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * ContextStorage.addWrapper 기반 MDC 주입은 등록 시점이 OTel Context 최초 사용보다 늦으면
 * 조용히 무시된다(이번에 실제로 그렇게 깨져 있었다 — Tempo엔 스팬이 정상 도착했지만 로그엔
 * traceId가 단 한 번도 안 찍혔다). 이 TurboFilter는 그 등록 순서에 의존하지 않는다:
 * 로그를 찍는 바로 그 순간 Span.current()로 "지금 활성 스팬"을 직접 물어봐서 MDC에 넣으므로,
 * 초기화 타이밍과 무관하게 항상 정확하다.
 */
public class TraceIdTurboFilter extends TurboFilter {

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		SpanContext spanContext = Span.current().getSpanContext();
		if (spanContext.isValid()) {
			MDC.put(LogFields.TRACE_ID, spanContext.getTraceId());
		} else {
			MDC.remove(LogFields.TRACE_ID);
		}
		return FilterReply.NEUTRAL;
	}
}
