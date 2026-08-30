# syntax=docker/dockerfile:1
#
# 빌드 컨텍스트는 이 레포 루트여야 한다(Gradle 멀티모듈이라 :services:identity-service:bootJar 는
# settings.gradle / build.gradle / gradle / services 를 함께 봐야 성립).
# 공통 코드는 groovy-common(GitHub Packages)으로 분리됐다 — build stage 의 gradle 이 이를
# 내려받도록 GITHUB_ACTOR/GPR_TOKEN 을 BuildKit secret 으로 주입한다(이미지 레이어에 안 남음).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY services services

RUN --mount=type=secret,id=gpr_actor --mount=type=secret,id=gpr_token \
	chmod +x gradlew && \
	GITHUB_ACTOR="$(cat /run/secrets/gpr_actor)" \
	GPR_TOKEN="$(cat /run/secrets/gpr_token)" \
	./gradlew :services:identity-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre AS run
WORKDIR /app

RUN groupadd --system groovy && useradd --system --gid groovy --no-create-home groovy

COPY --from=build /workspace/services/identity-service/build/libs/*-SNAPSHOT.jar app.jar
RUN chown groovy:groovy app.jar

USER groovy
EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
	CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
