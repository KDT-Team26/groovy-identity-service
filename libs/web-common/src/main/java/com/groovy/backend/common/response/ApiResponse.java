package com.groovy.backend.common.response;

public record ApiResponse<T>(String status, String message, T data) {

	public static <T> ApiResponse<T> of(String status, String message, T data) {
		return new ApiResponse<>(status, message, data);
	}

	public static <T> ApiResponse<T> of(String status, String message) {
		return new ApiResponse<>(status, message, null);
	}
}
