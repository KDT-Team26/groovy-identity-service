package com.groovy.backend.identity.dto;

import com.groovy.backend.identity.ProviderType;
import com.groovy.backend.identity.RoleType;
import com.groovy.backend.identity.User;

public record UserResponse(String id, String email, String name, ProviderType providerType, RoleType roleType) {

	public static UserResponse from(User user) {
		return new UserResponse(String.valueOf(user.getId()), user.getEmail(), user.getName(), user.getProviderType(), user.getRoleType());
	}
}
