package com.groovy.backend.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groovy.backend.identity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
