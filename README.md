# groovy-identity-service

## 1. Repo: groovy-identity-service

**Groovy**는 태그 기반으로 스터디 그룹을 매칭하고, 참여 신청/승인, 캘린더 일정 관리, 회고록 공유,
실시간 알림까지 지원하는 스터디 커뮤니티 플랫폼입니다. 

`groovy-identity-service`는 그중 **신원/인증/태그**를 담당하는 서비스입니다. 회원 가입·로그인·
로그아웃과 마이페이지 조회를 담당하고, **시스템 전체에서 유일한 JWT 발급자**로서 RSA 서명 키를
관리하며 다른 서비스가 검증에 쓸 공개키를 JWKS로 공개합니다. 또한 태그 마스터 목록과 회원별
선호 태그(취향 매칭에 쓰이는 데이터)도 이 서비스가 정본으로 소유합니다.

## 2. 주요 기능

- 회원가입 / 로그인(JWT 발급) / 로그아웃
- 내 정보 조회(마이페이지)
- 다른 서비스가 화면 표시용 이름을 배치 조회할 수 있는 내부 API
- JWKS(JSON Web Key Set) 공개 — 다른 5개 서비스가 이 공개키로 JWT 서명을 검증
- 태그 마스터 목록 조회, 회원별 선호 태그 조회/저장

## 3. 시스템 아키텍처

```
                         ┌────────────────────┐
 브라우저 ──▶ api-gateway ──▶│  identity-service   │
                         │  (:8081)            │
                         └─────────┬───────────┘
                                   │ JPA / Flyway
                                   ▼
                         MySQL: identity_db
                         (계정: identity_service, 이 서비스만 GRANT ALL)

  study / calendar / content / notification-service
    └─ GET /.well-known/jwks.json 로 공개키만 가져감(신뢰의 기반, 시크릿 공유 없음)
  study / content-service
    └─ GET /api/users/names, GET /api/tags/me 로 표시용 데이터 조회
```

### DB / 계정 / 테이블

| 항목 | 값 |
| --- | --- |
| DB(스키마)명 | `identity_db` |
| 전용 계정 | `identity_service` (이 스키마에만 `GRANT ALL`, 다른 서비스 DB 접근 불가) |
| 마이그레이션 | Flyway (`src/main/resources/db/migration`), 이 서비스가 자체 이력을 직접 소유 |

| 테이블 | 역할 | 비고 |
| --- | --- | --- |
| `users` | 회원 정본 | `email`(unique), `password`, `provider_type`(GOOGLE/KAKAO/LOCAL), `role_type`(ADMIN/USER). 다른 4개 서비스 DB가 이 `id`를 FK 없이 값으로만 참조 |
| `tags` | 태그 마스터 정본 | `category`(OPERATING_POLICY/STUDY_MODE), `name`(unique). **`study_db`에 FK 무결성용 로컬 사본이 별도로 존재**하며 실시간 동기화는 없음(수동 시드) |
| `user_tags` | 회원-태그 선호 관계 테이블 | `users`/`tags`와 같은 DB 내 FK, `identity_db` 안에서 완결 |

## 4. 기술 스택

- Java 21, Spring Boot 4.1.0, Gradle 멀티모듈
- Spring Security + Spring Data JPA + Bean Validation
- Flyway (MySQL) — `ddl-auto: validate`
- `jjwt` 0.12.6 — RSA 서명/검증, 자체 `TokenProvider` / `JwtKeyProvider` / `JwtAuthenticationFilter` / `JwksController`가 JWT 발급자 역할을 직접 구현(다른 서비스가 쓰는 `libs:security-common`은 검증 전용이라 이 서비스는 사용하지 않음)
- Micrometer + OpenTelemetry(OTLP) → Tempo 트레이싱, `/actuator/prometheus` 메트릭
- 공용 라이브러리: `libs:event-contract`(회원 탈퇴 이벤트 `UserDeletedPayload` 스키마만 선점 정의, 발행 코드는 아직 없음), `libs:observability`, `libs:web-common`
- Lombok

## 5. 다른 서비스와의 통신 관계

이 서비스는 **응답 제공자(provider, 다른 서비스를 호출하지 않음)** 입니다.

| 호출자 | 엔드포인트 | 용도 |
| --- | --- | --- |
| study, calendar, content, notification-service | `GET /.well-known/jwks.json` | JWT 서명 검증용 공개키(모든 서비스 신뢰의 기반) |
| study, content-service | `GET /api/users/names?ids=...` | 표시용 이름 배치 조회 |
| study-service | `GET /api/tags/me` | 태그 매칭 조회 시 선호 태그 대체값 |
| api-gateway | `Path=/api/auth/**`, `/api/users/me`(exact), `/api/tags/**` | 외부 요청 라우팅 |

## 6. 로컬 실행 방법

컨테이너로 직접 빌드:

```bash
# DB가 실행되고 있어야 함.
docker build -t groovy-identity-service .
docker run -p 8081:8081 \
  -e SPRING_DEV_DB_URL="jdbc:mysql://host.docker.internal:3306/identity_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul" \
  -e SPRING_DEV_DB_USERNAME=identity_service \
  -e SPRING_DEV_DB_PASSWORD=identity_service_local_only_pw \
  groovy-identity-service
```

기본 포트는 `8081`이며, `CORS_ALLOWED_ORIGINS`(기본 `http://localhost:5173`)로 프론트엔드 credentials 요청을 허용합니다.

## 7. 모니터링 스택에서 관측되는 부분

- **Prometheus**: `job=identity-service`로 `identity-service:8081/actuator/prometheus`를 15초 주기로 스크래핑. HikariCP 커넥션 풀(`hikaricp_connections_*`)은 서비스 단위로 정확히 분리되어 수집되지만, 공유 MySQL 컨테이너 자체 지표(`mysqld-exporter`)는 인스턴스 전체 단위라 `identity_db`만 따로 보기는 어렵습니다.
- **Alertmanager**: `HikariCpuPoolPendingDetected`(커넥션 풀 대기 발생), `BackendMemoryUsageTooHigh`(JVM 힙 40% 초과), `BackendCpuSpikeDetected`(CPU 95% 초과) 규칙이 이 서비스에도 동일하게 적용됩니다.
- **Grafana**: JVM(Micrometer) 대시보드와 Loki 기반 로그 대시보드가 프로비저닝되어 있습니다(HikariCP/MySQL 전용 패널은 아직 없음 — Prometheus Explore로만 확인 가능).
- **Tempo**: 이 서비스가 발급자로 관여하는 요청(로그인, JWKS 조회 등)의 트레이스가 api-gateway에서 시작해 W3C `traceparent`로 전파됩니다.
- **계약 테스트로서의 관측**: `JwksControllerContractTest`가 CI에 포함되어, 이 서비스가 내려주는 JWKS 응답 포맷이 다른 4개 서비스의 소비 코드와 조용히 어긋나지 않도록 검증합니다.
