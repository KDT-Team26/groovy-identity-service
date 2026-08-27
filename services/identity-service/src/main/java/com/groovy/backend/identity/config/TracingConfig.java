package com.groovy.backend.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.groovy.backend.observability.HttpStatusObservationFilter;

import io.micrometer.observation.ObservationFilter;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * MSA 전환 Phase 12(패턴 재사용): notification-service의 TracingConfig와 동일하다 — Boot 4.1의
 * spring-boot-opentelemetry/spring-boot-micrometer-tracing이 SdkTracerProvider/Tracer 빈을
 * 자동 생성해주지 않아 직접 조립한다.
 *
 * MDC에 traceId를 채우는 건 더 이상 여기서 안 한다 — ContextStorage.addWrapper는 등록
 * 시점이 OTel Context 최초 사용보다 늦으면 조용히 무효화되는 함정이 있고, 실제로 그렇게
 * 깨져서 Tempo엔 스팬이 잘 쌓이는데 로그엔 traceId가 전혀 안 찍히는 상태였다. 대신
 * observability 모듈의 TraceIdTurboFilter가 로그 찍는 순간 Span.current()를 직접 읽는다
 * (등록 순서에 의존하지 않아 더 견고하다).
 */
@Configuration
public class TracingConfig {

	@Bean
	public SdkTracerProvider sdkTracerProvider(
		Resource resource,
		@Value("${management.otlp.tracing.endpoint}") String otlpEndpoint,
		@Value("${management.tracing.sampling.probability:1.0}") double samplingProbability
	) {
		OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
			.setEndpoint(otlpEndpoint)
			.build();

		return SdkTracerProvider.builder()
			.setResource(resource)
			.setSampler(Sampler.traceIdRatioBased(samplingProbability))
			.addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
			.build();
	}

	@Bean
	public ContextPropagators contextPropagators() {
		return ContextPropagators.create(TextMapPropagator.composite(
			W3CTraceContextPropagator.getInstance(),
			W3CBaggagePropagator.getInstance()));
	}

	@Bean
	public Tracer tracer(OpenTelemetrySdk openTelemetrySdk, @Value("${spring.application.name}") String serviceName) {
		return new OtelTracer(openTelemetrySdk.getTracer(serviceName), new OtelCurrentTraceContext(), event -> { });
	}

	@Bean
	public Propagator propagator(OpenTelemetrySdk openTelemetrySdk, @Value("${spring.application.name}") String serviceName) {
		return new OtelPropagator(openTelemetrySdk.getPropagators(), openTelemetrySdk.getTracer(serviceName));
	}

	// Micrometer 기본 HTTP observation convention은 상태 코드를 "status"라는 축약 키로만 span에
	// 붙인다 — Tempo/Grafana가 흔히 찾는 OTel 표준 키(http.response.status_code)로도 남긴다.
	@Bean
	public ObservationFilter httpStatusObservationFilter() {
		return new HttpStatusObservationFilter();
	}
}
