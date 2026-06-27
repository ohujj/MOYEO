# Moyeo Server

CMC 모여(Moyeo) 프로젝트의 Spring Boot 기반 MVP 백엔드 서버입니다.

현재 서버는 기본 실행 환경, health check, Swagger/OpenAPI, 공통 오류 응답, 회원/로그인 기반 구조, dev 배포 환경을 포함합니다.

## Tech Stack

- Java 21
- Spring Boot 3.5.15
- Gradle
- Spring Web, Validation, Data JPA
- H2(local/test)
- MySQL(dev/prod)
- Springdoc OpenAPI
- Spring Boot Actuator
- JUnit 5
- Docker, Docker Compose
- AWS EC2, ECR, RDS MySQL, Systems Manager
- GitHub Actions

## Local Run

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Default local port:

```text
8080
```

## Test and Build

macOS/Linux:

```bash
./gradlew test
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## API Paths

Local:

- Health Check: `GET http://localhost:8080/health`
- Actuator Health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Dev Server:

- API Base URL: `http://3.35.119.70:8080`
- Health Check: `http://3.35.119.70:8080/health`
- Swagger UI: `http://3.35.119.70:8080/swagger-ui.html`
- OpenAPI JSON: `http://3.35.119.70:8080/v3/api-docs`

`GET /health` response:

```json
{
  "status": "OK"
}
```

## Current Auth APIs

The current auth implementation is a temporary MVP base.

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

Login responses include an Access JWT.
Protected APIs use the `Authorization: Bearer {accessToken}` header.

Not included yet:

- Refresh Token
- Logout
- Kakao/Apple OAuth integration
- Guest participant authentication

## Dev Deployment

The dev server is deployed on AWS.

```text
GitHub Actions
→ Gradle test/build
→ Docker image build
→ Amazon ECR push
→ AWS Systems Manager Run Command
→ EC2 Docker Compose deployment
→ RDS MySQL connection
```

Runtime components:

- EC2: `moyeo-api-dev`
- RDS MySQL: `moyeo-dev-db`
- ECR repository: `moyeo-server`
- App container: `moyeo-server`

Security policy for dev:

- API port `8080` is public for frontend development and testing.
- SSH port `22` is restricted to the developer IP.
- RDS MySQL port `3306` is not publicly exposed.
- GitHub Actions deploys through AWS Systems Manager instead of opening SSH to GitHub Actions runners.

## Environment Variables

`dev` and `prod` profiles require environment variables.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
```

Dev CORS origin example:

```text
CORS_ALLOWED_ORIGINS=https://moyeo-web.vercel.app,https://moyeo-dev.vercel.app,http://localhost:3000
```

The EC2 dev server stores runtime values in:

```text
/home/ubuntu/moyeo/.env
```

Do not commit real secrets to the repository.

## Documentation

- Codex working rules: `AGENTS.md`
- Project setup and technical decisions: `docs/00-project-setup.md`
- DB diagram DBML: `docs/01-dbdiagram.md`
