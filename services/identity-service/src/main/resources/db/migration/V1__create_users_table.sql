-- Phase 7(DB per Service) 패턴 재사용: identity_db에 처음으로 만드는 테이블. groovy(레거시)의
-- V1__baseline_schema.sql이 만든 users 테이블과 컬럼은 동일하되, groovy_db와 물리적으로 분리된
-- 별개의 스키마다(각자 own AUTO_INCREMENT를 갖는다 — 기존 groovy_db.users의 id와 이 테이블의
-- id는 서로 다른 시퀀스라는 뜻이다. 알려진 한계는 application.yml 주석 참고).
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `provider_type` enum('GOOGLE','KAKAO','LOCAL') NOT NULL,
  `role_type` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
