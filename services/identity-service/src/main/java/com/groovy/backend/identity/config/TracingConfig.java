package com.groovy.backend.identity.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
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
 */
@Configuration
public class TracingConfig {

	private final List<EventListener> eventListeners = List.of(
		new Slf4JEventListener(),
		new Slf4JBaggageEventListener(List.of())
	);

	public TracingConfig() {
		ContextStorage.addWrapper(new EventPublishingContextWrapper(
			event -> eventListeners.forEach(listener -> listener.onEvent(event))));
	}

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
		OtelTracer.EventPublisher eventPublisher = event ->
			eventListeners.forEach(listener -> listener.onEvent(event));
		return new OtelTracer(openTelemetrySdk.getTracer(serviceName), new OtelCurrentTraceContext(), eventPublisher);
	}

	@Bean
	public Propagator propagator(OpenTelemetrySdk openTelemetrySdk, @Value("${spring.application.name}") String serviceName) {
		return new OtelPropagator(openTelemetrySdk.getPropagators(), openTelemetrySdk.getTracer(serviceName));
	}
}
