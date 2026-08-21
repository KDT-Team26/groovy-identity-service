-- MSA 전환(Tag 소유권 확정): Tag 마스터 + UserTag(선호 태그)의 정본을 identity-service가
-- 소유한다. groovy(레거시)의 V1__baseline_schema.sql이 만든 tags/user_tags 테이블과 컬럼
-- 구성은 동일하되, identity_db와 groovy_db/study_db는 물리적으로 분리된 별개의 스키마다
-- (각자 own AUTO_INCREMENT — 알려진 한계, study-service application.yml 주석 참고).
CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('OPERATING_POLICY','STUDY_MODE') NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `tag_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`,`tag_id`),
  KEY `fk_user_tags_tag_id` (`tag_id`),
  CONSTRAINT `fk_user_tags_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`),
  CONSTRAINT `fk_user_tags_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
