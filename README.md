# 다믿어? 백엔드

AI가 내민 두 답 중 지어낸 쪽을 골라내는 2지선다 게임 "다믿어?"의 API 서버입니다.

## 스택

- Java 21, Spring Boot 3.5, Gradle
- MySQL 8.0, Spring Data JPA, Flyway
- springdoc-openapi (Swagger UI)
- 테스트: JUnit 5, Testcontainers(MySQL)

## 로컬 실행

1. 로컬 MySQL 띄우기 (Docker 필요)

```
docker compose up -d
```

2. 서버 실행 (기본 프로필 `local`)

```
./gradlew bootRun
```

3. 확인

- 헬스체크: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

로컬 DB 접속 정보는 `application-local.yml`의 기본값(damideo / damideo)을 쓰고, 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 바꿀 수 있습니다.

## 테스트

```
./gradlew test
```

테스트는 Testcontainers로 MySQL 8.0 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

## 공통 규칙

- 인증: 로그인 없이 게스트 계정을 씁니다. 게스트 계정 생성을 빼고 모든 요청에 `X-Device-Id` 헤더(기기 UUID)를 보냅니다.
- 응답 형식

```json
{ "success": true, "code": "...", "message": "...", "data": { } }
{ "success": false, "code": "...", "message": "...", "errors": { } }
```

- 시간은 DB에 UTC로 저장하고, "오늘"과 날짜 계산은 KST 기준입니다.
- DB 변경은 `src/main/resources/db/migration`에 Flyway 파일(`V{번호}__설명.sql`)로만 합니다. 이미 머지된 파일은 수정하지 않습니다.

## 패키지 구조

```
com.unid8.damideo
 ├─ common     공통 응답, 예외, 설정
 ├─ user       게스트 계정, 닉네임, 연속 기록
 ├─ question   문제은행
 ├─ daily      데일리 5문제
 ├─ game       실전 판, 연승 챌린지
 ├─ answer     답 제출, 오답노트
 └─ stats      약점 유형, 나의 성장, 랭킹
```

## 배포 환경변수

`.env.example`의 키를 GitHub 시크릿 `ENV_FILE`에 넣어 씁니다. 실제 값은 레포에 올리지 않습니다.

## 협업 규칙

[CONTRIBUTING.md](CONTRIBUTING.md)를 참고해주세요.
