# Project Setup Policy

> Last reviewed: 2026-06-21
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

## Today's Scope

- Basic server runtime
- Health check
- Swagger/OpenAPI
- Actuator
- Local profile
- H2/MySQL configuration template
- CI
- `AGENTS.md`
- `README.md`

## Tech Decisions

### Java 21

- Use an LTS release for stability and long-term maintainability in a new project.
- Java 8/11 remain meaningful for legacy maintenance but offer little reason for selection in a new MVP.

### Spring Boot 3.5.x

- Prioritize stability, reference accessibility, and ecosystem compatibility for a six-week MVP and team collaboration.

### JPA

- Appointment rooms, participants, votes, responses, and results are expected to have clear relationships, making a relational model and ORM a suitable candidate.
- No entity or domain model is defined during initial setup.

### MySQL

- Use as the candidate database for real dev/prod environments.
- Prefer its relational consistency and broad operational references.

### H2

- Use only for local development and tests.
- Do not use H2 as the production database.

## Currently Excluded

### NoSQL

- Current MVP data appears relational.
- No document or key-value storage requirement has been established.

### Redis/Kafka/WebSocket

- Traffic, real-time, and event-processing requirements are not yet clear for the six-week MVP.
- Revisit after operational needs are validated.

### Nginx/Blue-Green/Zero-downtime Deployment

- Consider operational needs, but do not implement them today.
- Revisit after MVP completion during operational hardening.

### MCP/Sub-agents/Complex Hooks

- Codex working rules, documentation, and CI are sufficient for now.
- Revisit only when a concrete need appears.

## AI-assisted Development Policy

- Codex assists with repetitive work and initial setup.
- Humans decide domain policy and technical direction.
- Codex does not create policies absent from documentation.
- Humans review AI-generated code.
- Work is not complete until build and tests pass.
- Manage this workflow as AI-assisted development from a harness-engineering perspective.

## API Response and Error Policy

- Return successful API responses as their response DTO without a common success wrapper.
- Express API errors as RFC 9457 Problem Details with the `application/problem+json` media type.
- Add a stable `code` property for client-side branching and an `errors` property only for validation details.
- Do not expose rejected values, exception types, stack traces, or internal exception messages.
- Keep `/health`, Actuator, and Swagger/OpenAPI responses in their native formats.
- Define domain error codes only after the related domain policy is documented.

## Authentication Policy

- Keep `User` as the service user identity.
- Keep local login credentials in `LoginAccount` instead of storing password data directly on `User`.
- Keep social provider identities in `SocialAccount` using `provider + providerUserId`.
- Use an Access JWT for local signup/login responses.
- Keep the current JWT implementation minimal: no refresh token, logout, token rotation, guest token, or authorization filter yet.
- Store real JWT secrets through environment variables in dev/prod.
- Do not store CI/DI unless a separate human decision, consent policy, and security policy are documented.

## Deployment Policy

- Use Docker for a repeatable dev deployment artifact.
- Use AWS EC2 as the first dev deployment target.
- Use Amazon ECR for private Docker image storage.
- Use GitHub Actions for build, test, image push, and EC2 deployment automation.
- Keep dev/prod secrets in GitHub Secrets or AWS-managed secret storage, not in repository files.
- Keep zero-downtime deployment, blue/green deployment, load balancer setup, and autoscaling out of the current MVP setup.
- Revisit RDS, HTTPS, reverse proxy, migration, rollback, and zero-downtime strategy before public launch.

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

- Docker
- Nginx reverse proxy
- Blue/Green or Rolling deployment
- Zero-downtime deployment
- Database migration
- traceId-based logging
- Error monitoring
- Deployment automation
- Operational metrics collection
