# Project Setup Policy

> Last reviewed: 2026-06-27
> Review trigger: 기술 스택, MVP 범위, 배포 방식, Codex 작업 규칙, 도메인 정책 변경 시

## Project Goal

- Develop the six-week MVP server for the CMC Moyeo project.
- Prioritize completing the core user flow and reaching a deployable state.
- Consider later operational maturity without building excessive infrastructure up front.

## MVP Priority

The following is a candidate core flow for later implementation, not today's implementation scope.

```text
Create appointment room
Share link
Collect participant responses
Close voting
View results
Finalize decision
```

## Current Scope

- Basic server runtime
- Health check
- Swagger/OpenAPI
- Actuator
- Local profile
- H2 local datasource
- MySQL dev/prod datasource configuration
- GitHub Actions CI/CD
- Common error response base
- Member/login base entities
- Temporary local login API and Access JWT issue flow
- AWS dev deployment

## Tech Decisions

### Java 21

- Use an LTS release for stability and long-term maintainability in a new project.
- Java 8/11 remain meaningful for legacy maintenance but offer little reason for selection in a new MVP.

### Spring Boot 3.5.x

- Prioritize stability, reference accessibility, and ecosystem compatibility for a six-week MVP and team collaboration.

### JPA

- Appointment rooms, participants, votes, responses, and results are expected to have clear relationships, making a relational model and ORM a suitable candidate.
- Current implemented entities are limited to member/login base structures.

### MySQL

- Use MySQL as the candidate database for real dev/prod environments.
- Dev server uses Amazon RDS MySQL.

### H2

- Use only for local development and tests.
- Do not use H2 as the production database.

## Currently Excluded

### Domain Logic

- Appointment-room domain logic
- Voting domain logic
- Participant domain logic
- Result domain logic

Do not implement domain behavior until a human-defined policy exists.

### Redis/Kafka/WebSocket/NoSQL

- Current MVP data appears relational.
- Traffic, real-time, and event-processing requirements are not yet clear for the six-week MVP.
- Revisit after operational needs are validated.

### Nginx/Blue-Green/Zero-downtime Deployment

- Consider operational needs, but do not implement them in the current dev setup.
- Revisit before public launch or operational hardening.

### MCP/Sub-agents/Complex Hooks

- Codex working rules, documentation, and CI/CD are sufficient for now.
- Revisit only when a concrete need appears.

## AI-assisted Development Policy

- Codex assists with repetitive implementation work inside human-defined boundaries.
- Humans decide domain policy, product policy, technical direction, and completion criteria.
- Codex must not create or implement policies absent from documentation.
- Features with undecided domain policy must remain unimplemented or be left as a proposal/TODO.
- Humans review AI-generated changes before treating them as complete.

Codex may help with:

- Repetitive implementation
- Boilerplate code
- Test drafts
- Documentation drafts
- Swagger/OpenAPI descriptions
- Simple refactoring within the requested scope

Codex must not do the following without explicit human direction:

- Create undocumented domain policy
- Change authentication or security policy
- Change deployment architecture
- Change API response rules
- Expand the database schema beyond the requested scope

AI-assisted work is complete only when:

- Build succeeds.
- Tests pass.
- Swagger/OpenAPI update needs are checked.
- API response and error policies are respected.
- A human reviews the change scope and checks for policy violations.

The development harness includes GitHub Actions CI/CD, Swagger/OpenAPI, the current RFC 9457-based error response policy, and documented working rules.

## API Response and Error Policy

- Return successful API responses as their response DTO without a common success wrapper.
- Express API errors as RFC 9457 Problem Details with the `application/problem+json` media type.
- Add a stable `code` property for client-side branching and an `errors` property only for validation details.
- Do not expose rejected values, exception types, stack traces, or internal exception messages.
- Keep `/health`, Actuator, and Swagger/OpenAPI responses in their native formats.
- Define domain error codes only after the related domain policy is documented.
- Current common error handling is temporary until domain-specific errors are defined.

## API Documentation Policy

- Use Swagger/OpenAPI as the primary API contract reference.
- When adding or changing APIs, keep Swagger easy to read for frontend collaboration.
- Add operation summaries, descriptions, DTO field descriptions, and example values when they help API consumers.
- Keep Notion focused on product flow, policies, decisions, and links to Swagger rather than duplicating every request/response shape.

## Authentication Policy

- Keep `User` as the service user identity.
- Keep local login credentials in `LoginAccount` instead of storing password data directly on `User`.
- Keep social provider identities in `SocialAccount` using `provider + providerUserId`.
- Use an Access JWT for local signup/login responses and protected API authentication.
- Keep the current JWT implementation minimal: no refresh token, logout, token rotation, or guest token yet.
- Store real JWT secrets through environment variables in dev/prod.
- Configure CORS with explicit frontend origins and update them when frontend deployment URLs are decided.
- If `CORS_ALLOWED_ORIGINS` exists in the EC2 runtime `.env`, it overrides the default origins in `application-dev.yml`.
- Do not store CI/DI unless a separate human decision, consent policy, and security policy are documented.

## Deployment Policy

- Use Docker for a repeatable dev deployment artifact.
- Use AWS EC2 as the first dev deployment target.
- Use Amazon RDS MySQL as the dev database.
- Use Amazon ECR for private Docker image storage.
- Use GitHub Actions for build, test, image push, and EC2 deployment automation.
- Prefer AWS Systems Manager Run Command over opening SSH to GitHub Actions runners.
- Keep EC2 runtime secrets in a server-side `.env` file or managed secret storage instead of passing them through deployment commands.
- Keep dev/prod secrets in GitHub Secrets or AWS-managed secret storage, not in repository files.
- Keep dev API port `8080` public for frontend collaboration.
- Keep SSH port `22` restricted to the developer IP.
- Keep RDS MySQL port `3306` private and accessible only from the EC2 application path.
- Keep zero-downtime deployment, blue/green deployment, load balancer setup, and autoscaling out of the current MVP setup.
- Revisit HTTPS, reverse proxy, migration, rollback, and zero-downtime strategy before public launch.

## Current Dev Infrastructure

- Dev API base URL: `http://3.35.119.70:8080`
- EC2 instance: `moyeo-api-dev`
- Elastic IP: `3.35.119.70`
- RDS instance: `moyeo-dev-db`
- ECR repository: `moyeo-server`
- Deployment workflow: `.github/workflows/deploy-dev.yml`
- Runtime env file on EC2: `/home/ubuntu/moyeo/.env`
- Deployment command path: GitHub Actions -> Amazon ECR -> AWS Systems Manager Run Command -> EC2 Docker Compose
- Repository mirrors: push verified `main` changes to both `origin` and `cmc` while the personal and CMC repositories are maintained together.

## Documentation Policy

- Keep documentation minimal and useful.
- AGENTS.md is the primary working rule for Codex.
- README.md is for humans: setup, run commands, API paths, and basic project information.
- docs/00-project-setup.md is for project-level decisions, such as tech choices, excluded technologies, and operational roadmap.
- docs/01-dbdiagram.md is for the current database schema in dbdiagram.io DBML format.
- Do not create new markdown documents unless the topic is stable enough to maintain.
- For feature-level work, prefer adding a short policy section only when the policy is actually needed.
- Do not create separate workflow or review checklist documents unless the project complexity justifies them.

## Operational Roadmap

After MVP completion, review these items in sequence as needs become clear:

- HTTPS and domain
- Nginx or Caddy reverse proxy
- Database migration
- Refresh Token and token rotation
- traceId-based logging
- Error monitoring
- Deployment rollback strategy
- Blue/Green or rolling deployment
- Zero-downtime deployment
- Operational metrics collection
