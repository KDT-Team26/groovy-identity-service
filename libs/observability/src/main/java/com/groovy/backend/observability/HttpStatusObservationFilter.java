package com.groovy.backend.observability;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Micrometer의 기본 HTTP observation convention은 상태 코드를 "status"라는 짧은 키로만 span에
 * 붙인다(Tempo에서 직접 확인함). Grafana Tempo UI/TraceQL은 OTel 표준 키(http.response.status_code)를
 * 찾는 경우가 많아서, 같은 값을 표준 키로도 하나 더 붙여준다.
 */
public class HttpStatusObservationFilter implements ObservationFilter {

	private static final String STATUS_CODE_KEY = "http.response.status_code";

	@Override
	public Observation.Context map(Observation.Context context) {
		if (context instanceof ServerRequestObservationContext serverContext) {
			HttpServletResponse response = serverContext.getResponse();
			if (response != null) {
				context.addLowCardinalityKeyValue(KeyValue.of(STATUS_CODE_KEY, String.valueOf(response.getStatus())));
			}
		} else if (context instanceof ClientRequestObservationContext clientContext) {
			ClientHttpResponse response = clientContext.getResponse();
			if (response != null) {
				try {
					context.addLowCardinalityKeyValue(
						KeyValue.of(STATUS_CODE_KEY, String.valueOf(response.getStatusCode().value())));
				} catch (IOException ignored) {
					// 응답 스트림이 이미 소비/종료된 극히 드문 경우 — 상태 코드 태깅만 건너뛴다.
				}
			}
		}
		return context;
	}
}
