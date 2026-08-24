-- MSA 전환 Phase 7(DB per Service): 하나의 MySQL 컨테이너 안에서 스키마 소유권부터 분리한다.
-- legacy-monolith(groovy_db)는 삭제됐다. 여기서는 identity-service 전용 스키마 + 그 스키마에만
-- 접근 가능한 전용 계정을 추가로 만든다.
--
-- 소유권 이전(groovy-infra#7): 이 스크립트는 원래 groovy-infra/mysql-init/02-identity-service.sql로
-- platform(mysql)이 소유하고 있었으나, platform이 서비스 Secret을 역참조하는 문제를 없애기 위해
-- 이 레포로 이전했다. 실행 메커니즘(누가/언제 이 스크립트를 돌릴지)은 아직 연결되지 않았고
-- 후속 이슈에서 다룬다 — 지금은 파일 소유권 이전까지만이 이번 작업 범위다.
--
-- 알려진 한계(문서화된 임시 상태): identity_db.users는 groovy_db.users와 물리적으로 분리된 별개의
-- 스키마다. identity-service 도입 이후 새로 가입하는 유저는 여기에만 생성되고 groovy_db에는
-- 반영되지 않는다 — groovy(레거시)에 남은 Study/Calendar/Memoir 등은 그 이후 가입한 유저를
-- 조회하지 못한다. 나머지 도메인이 실제로 추출되어 groovy_db.users 의존을 완전히 제거할 때
-- 함께 해소된다(application.yml 주석 참고).

CREATE DATABASE IF NOT EXISTS identity_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'identity_service'@'%'
  IDENTIFIED BY 'identity_service_local_only_pw';

-- identity_service 계정은 identity_db에만 권한이 있다. groovy_db(legacy가 쓰는
-- study/user/memoir/calendar 테이블들)에는 아무 권한도 주지 않는다.
GRANT ALL PRIVILEGES ON identity_db.* TO 'identity_service'@'%';

FLUSH PRIVILEGES;
