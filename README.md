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
- AWS EC2, ECR, EC2 Docker Compose MySQL, Systems Manager
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

When the `local` or `dev` profile is active, the server creates these idempotent test
accounts and exposes one token endpoint:

- `POST /api/auth/dev/tokens`

The endpoint requires no request body and returns the Access JWT responses for
`dev-user-1` and `dev-user-2`. It is not registered in the `prod` profile.

Login responses include an Access JWT.
Protected APIs use the `Authorization: Bearer {accessToken}` header.

Not included yet:

- Refresh Token
- Logout
- Kakao/Apple OAuth integration
- Guest participant authentication

## Current Meeting APIs

The current meeting implementation covers the first milestone base flow.

- `POST /api/meetings`
- `GET /api/meetings/invitations/{inviteCode}`
- `POST /api/meetings/invitations/{inviteCode}/guests`
- `PUT /api/meetings/invitations/{inviteCode}/participants/{participantId}/participation`

Current meeting scope:

- A logged-in user can create a meeting as host.
- Meeting creation for the first MVP accepts the first creation flow settings in one request.
- The server issues an invite code.
- INV-01 invite entry uses public invite-code lookup and returns meeting basic information plus participation availability status.
- A guest can join with nickname and password.
- Guest join does not accept departure address, coordinates, or transportation mode directly.
- Participant nicknames are unique only inside each meeting.
- `deadlineAt` is calculated by the server from request `deadlineMinutes`.
- `deadlineMinutes` is accepted in 10-minute units from 10 minutes up to 72 hours.
- Schedule voting applies the same available time range to every selected candidate date.
- Schedule voting time ranges are accepted in 1-hour units.
- Guest participation is rejected after `deadlineAt`.
- Invite-code lookup returns whether the current meeting can still be joined and the reason/message when joining is blocked.
- Schedule/place coordination modes are stored, but recommendation calculation is not implemented yet.
- Middle-point creation stores the host departure name, address, coordinates, and transportation mode as the host participant snapshot.
- Place recommendation strategy is fixed after meeting creation in the first MVP.
- INV-02 participation input stores schedule availability for schedule-coordination meetings.
- INV-02 participation input stores departure address, coordinates, and transportation mode for place-coordination meetings.
- A participation save request replaces the participant's previous schedule availability slots.
- Schedule result logic, including intersection calculation and sorting by longest meeting time or earliest date, is not implemented yet.

Not included yet:

- Step-by-step meeting draft save
- Schedule coordination beyond participant availability input
- Place coordination beyond participant departure input
- Current-location lookup and saved departure-list management
- Tmap/Tmap Transit integration
- Store-area/place recommendation data
- Voting/free-poll
- Final decision/result
- Meeting list/detail tabs
- Meeting edit/delete
- Guest re-entry authentication

## Dev Deployment

The dev server is deployed on AWS.

The dev profile currently uses Hibernate schema update while the MVP schema is still changing. Treat this as temporary development convenience, not a production migration strategy.

```text
GitHub Actions
→ Gradle test/build
→ Docker image build
→ Amazon ECR push
→ AWS Systems Manager Run Command
→ EC2 Docker Compose deployment
→ EC2 Docker Compose MySQL connection
```

Runtime components:

- EC2: `moyeo-api-dev`
- MySQL container: `moyeo-mysql`
- ECR repository: `moyeo-server`
- App container: `moyeo-server`

Security policy for dev:

- API port `8080` is public for frontend development and testing.
- SSH port `22` is restricted to the developer IP.
- MySQL port `3306` is not publicly exposed.
- MySQL may be bound to EC2 localhost `127.0.0.1:3306` for DBeaver access through SSH tunneling.
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
