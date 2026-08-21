package com.groovy.backend.identity.dto;

import com.groovy.backend.identity.Tag;
import com.groovy.backend.identity.TagCategory;

public record TagResponse(Long id, String name, TagCategory category) {

	public static TagResponse from(Tag tag) {
		return new TagResponse(tag.getId(), tag.getName(), tag.getCategory());
	}
}
