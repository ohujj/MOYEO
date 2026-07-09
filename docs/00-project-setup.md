# Project Setup Policy

> Last reviewed: 2026-07-09
> Review trigger: 기술 스택, MVP 범위, 배포 방식, Codex 작업 규칙, 도메인 정책 변경 시

## Project Goal

- Moyeo is a product intended for continued development and operation.
- The current six-week MVP is the initial delivery milestone, not the expected
  lifetime of the project.
- Core MVP delivery is the current priority.
- The server must remain deployable and maintainable.
- Avoid premature infrastructure and speculative abstractions.
- Avoid disposable MVP shortcuts that create unnecessary schema, API, migration,
  or operational debt.
- Prefer simple designs that can evolve incrementally when requirements are
  confirmed.

## Project Lifecycle Context

The first milestone is a six-week MVP phase. After the MVP, product requirements
are expected to be validated and evolved through continued development and
operation.

This context should not be used as a reason to introduce enterprise-scale,
high-traffic, large-system, or microservice-readiness assumptions before those
requirements are confirmed. It should also not be used as a reason to create
undocumented temporary behavior or disposable implementations that conflict with
confirmed product direction.

## MVP Priority

The following is a candidate core flow for later implementation, not today's
implementation scope.

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
- First milestone room creation, invite-code lookup, and guest participation flow
- AWS dev deployment

## Tech Decisions

### Java 21

- Use an LTS release for stability and long-term maintainability in a new
  project.
- Java 8/11 remain meaningful for legacy maintenance but offer little reason for
  selection in a new MVP.

### Spring Boot 3.5.x

- Prioritize stability, reference accessibility, and ecosystem compatibility for
  a six-week MVP and team collaboration.

### JPA

- Appointment rooms, participants, votes, responses, and results are expected to
  have clear relationships, making a relational model and ORM a suitable
  candidate.
- Current implemented entities are limited to member/login base structures.

### MySQL

- Use MySQL as the candidate database for real dev/prod environments.
- Dev server currently uses MySQL 8.4 running in Docker Compose on the EC2 dev
  instance to reduce early MVP infrastructure cost.
- Dev server temporarily uses Hibernate `ddl-auto=update` while the MVP schema
  is still changing quickly. Revisit this before real user data matters and move
  to explicit migrations or `validate`.
- Amazon RDS MySQL is not the current default dev database. Keep any remaining
  RDS notes as legacy/reference only.

### H2

- Use only for local development and tests.
- Do not use H2 as the production database.

## Currently Excluded

### Domain Logic

- Appointment-room domain logic beyond the first milestone room/invite/guest
  participation base
- Voting domain logic
- Participant domain logic
- Result domain logic

Do not implement domain behavior until a human-defined policy exists.

### Redis/Kafka/WebSocket/NoSQL

- Current MVP data appears relational.
- Traffic, real-time, and event-processing requirements are not yet clear for the
  six-week MVP.
- Revisit after operational needs are validated.

### Nginx/Blue-Green/Zero-downtime Deployment

- Consider operational needs, but do not implement them in the current dev setup.
- Revisit before public launch or operational hardening.

### MCP/Sub-agents/Complex Hooks

- Codex working rules, documentation, and CI/CD are sufficient for now.
- Revisit only when a concrete need appears.

## Policy References

- API and error contract: `docs/policies/API_POLICY.md`
- Authentication and security: `docs/policies/AUTH_POLICY.md`
- Room and participation domain:
  `docs/policies/ROOM_PARTICIPATION_POLICY.md`
- AI code review: `docs/ai/CODE_REVIEW.md`

## AI-assisted Development Policy

- Codex operates inside human-defined boundaries.
- Humans own domain, product, and technical policy decisions.
- AI-generated changes require human review.
- Detailed review behavior is defined in `docs/ai/CODE_REVIEW.md`.
- Codex may assist with implementation, boilerplate, test drafts,
  documentation drafts, Swagger/OpenAPI descriptions, and simple refactoring
  within the requested scope.

The development harness includes GitHub Actions CI/CD, Swagger/OpenAPI, the
current RFC 9457-based error response policy, and documented working rules.

## Deployment Policy

- Use Docker for a repeatable dev deployment artifact.
- Use AWS EC2 as the first dev deployment target.
- Use MySQL 8.4 in Docker Compose on the EC2 dev instance as the current dev
  database.
- Use temporary Hibernate schema update only for the dev profile while MVP schema
  churn is high; do not use it as the production migration strategy.
- Keep the dev MySQL container private to the EC2 Docker network; do not expose
  port `3306` publicly.
- Binding MySQL to `127.0.0.1:3306` on EC2 is allowed for developer DBeaver
  access through SSH tunneling only.
- RDS is legacy/reference only for the current dev setup and may be revisited
  later if managed database reliability becomes more important than early cost
  control.
- Use Amazon ECR for private Docker image storage.
- Use GitHub Actions for build, test, image push, and EC2 deployment automation.
- Prefer AWS Systems Manager Run Command over opening SSH to GitHub Actions
  runners.
- Keep EC2 runtime secrets in a server-side `.env` file or managed secret
  storage instead of passing them through deployment commands.
- Keep dev/prod secrets in GitHub Secrets or AWS-managed secret storage, not in
  repository files.
- Keep dev API port `8080` public for frontend collaboration.
- Keep SSH port `22` restricted to the developer IP.
- Keep MySQL port `3306` private and accessible only from the EC2 application
  path.
- Keep zero-downtime deployment, blue/green deployment, load balancer setup, and
  autoscaling out of the current MVP setup.
- Revisit HTTPS, reverse proxy, migration, rollback, and zero-downtime strategy
  before public launch.

## Current Dev Infrastructure

- Dev API base URL: `http://3.35.119.70:8080`
- EC2 instance: `moyeo-api-dev`
- Elastic IP: `3.35.119.70`
- Dev database: MySQL 8.4 container `moyeo-mysql` on the EC2 Docker Compose
  network
- ECR repository: `moyeo-server`
- Deployment workflow: `.github/workflows/deploy-dev.yml`
- Runtime env file on EC2: `/home/ubuntu/moyeo/.env`
- Deployment command path: GitHub Actions -> Amazon ECR -> AWS Systems Manager
  Run Command -> EC2 Docker Compose
- Runtime `DB_URL` on the EC2 app container should point to the Compose service
  name:
  `jdbc:mysql://mysql:3306/moyeo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`.
- Repository mirrors: push verified `main` changes to both `origin` and `cmc`
  while the personal and CMC repositories are maintained together.

## Documentation Policy

- Keep documentation minimal and useful.
- AGENTS.md is the primary working rule for Codex.
- README.md is for humans: setup, run commands, API paths, and basic project
  information.
- docs/00-project-setup.md is for project-level decisions, such as tech choices,
  excluded technologies, and operational roadmap.
- docs/01-dbdiagram.md is for the current database schema in dbdiagram.io DBML
  format.
- docs/policies/ contains canonical API, auth, and domain policy documents.
- docs/ai/ contains AI review rules and reusable review feedback.
- Keep entity comments and DBML notes concise, useful, and aligned when schema
  meaning changes.
- Do not create new markdown documents unless the topic is stable enough to
  maintain.
- For feature-level work, prefer adding a short policy section only when the
  policy is actually needed.
- Do not create separate workflow or review checklist documents unless the
  project complexity justifies them.

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
