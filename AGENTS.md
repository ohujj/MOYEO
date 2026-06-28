# Codex Working Rules

## Role

- Codex is an implementation support tool.
- A human is the final decision-maker for product policy, domain policy, technical direction, and completion criteria.
- Codex may implement only within documented or explicitly requested boundaries.

## Source of Truth

- Read `docs/00-project-setup.md` before starting work.
- During feature development, read the relevant feature policy or user-provided requirement before implementation.
- Do not invent or implement policies that are not documented or explicitly approved.
- If a requirement is unclear, ask for clarification or leave it as a TODO/proposal instead of guessing.

## Current Project State

- Java 21, Spring Boot 3.5.x, Gradle.
- Local profile uses H2.
- Dev/prod profiles use MySQL configuration through environment variables.
- Dev server is deployed to AWS EC2 with Docker, Amazon ECR, RDS MySQL, and GitHub Actions.
- Deployment uses AWS Systems Manager Run Command instead of opening SSH to GitHub Actions runners.
- API documentation is served through Swagger/OpenAPI.
- Current API error handling uses RFC 9457 Problem Details as a temporary common error response base.
- Current auth implementation supports local signup/login, Access JWT issue, current user lookup, and explicit CORS origins.
- Current room implementation supports basic room creation, invite-code lookup, and guest participation for the first milestone.
- Current repositories are mirrored to both:
  - `origin`: personal repository
  - `cmc`: CMC organization repository

## Current 1st Milestone

The agreed first product flow is:

```text
Local login
→ room creation
→ invite link creation
→ invite link lookup
→ guest participation
```

Server-side implementation should prioritize:

- `Room`
- `RoomParticipant`
- invite code/link
- room-level participant nickname uniqueness
- basic room creation and lookup APIs

The current implementation covers this first milestone base flow only.
Schedule coordination, place coordination, voting, and final decision remain deferred.

## Allowed Scope

The following areas are currently allowed when explicitly requested:

- Health check
- Swagger/OpenAPI
- Actuator
- Local/dev/prod profile configuration
- H2 local/test datasource
- MySQL dev/prod datasource configuration
- GitHub Actions CI/CD
- Docker-based dev deployment
- SSM-based EC2 deployment
- Common error response base
- Member/login base entities
- Local signup/login API
- Access JWT issue and current user lookup
- Explicit CORS configuration
- API documentation annotations and examples
- DBML updates in `docs/01-dbdiagram.md`
- The first milestone room/invite/guest participation flow

## Forbidden or Deferred Scope

Do not implement the following until a human explicitly requests it and the policy is clear:

- Undocumented domain behavior
- Appointment-room policy beyond the first milestone
- Schedule coordination policy
- Place coordination policy
- Voting/free-poll policy
- Result/final-decision policy
- Group/community feature
- Guest token policy beyond room participation
- Refresh Token
- Logout
- Token rotation
- Kakao/Apple OAuth integration
- Phone verification
- CI/DI storage
- Redis
- Kafka
- NoSQL
- WebSocket
- MCP
- Sub-agents
- Complex hooks
- Nginx/reverse proxy
- Blue/Green deployment
- Zero-downtime deployment
- Autoscaling/load balancer setup
- Production secrets
- Destructive commands

## Authentication and Participant Policy

- `User` is the service user identity.
- `LoginAccount` stores local login credentials separately from `User`.
- `SocialAccount` stores provider identities using `provider + providerUserId`.
- `User.nickname` is not a global unique business key.
- A participant's display name inside a room should be handled by `RoomParticipant.nickname`.
- Room-level nickname duplication should be prevented with a room-scoped rule such as `unique(room_id, nickname)` when `RoomParticipant` is implemented.
- Current JWT scope is Access Token only.
- Do not add Refresh Token, logout, or token rotation without a separate policy decision.

## API and Swagger Policy

- Successful API responses should return their DTO directly without a common success wrapper.
- Current error responses use Problem Details with a stable `code` property.
- The error response format is still temporary and may be revisited later.
- Swagger/OpenAPI is the primary API contract reference.
- When adding or changing APIs, add useful Swagger summaries, descriptions, status-code responses, DTO field descriptions, and examples.
- Keep Notion focused on product flow, policies, decisions, and links to Swagger rather than duplicating every request/response shape.

## Database and DBML Policy

- If JPA entities, table names, columns, indexes, unique constraints, or relationships change, update `docs/01-dbdiagram.md`.
- Keep DBML copy-paste ready for dbdiagram.io.
- Dev may temporarily use `ddl-auto=update` for early schema iteration.
- Prod must not rely on unsafe automatic schema changes.
- Schema rename/delete/type-change work should be handled deliberately, preferably with migration planning.

## Deployment and Repository Policy

- Codex must not occupy port `8080` for temporary HTTP verification.
- If Codex runs the app for temporary HTTP verification, use `--server.port=18080` and stop the process immediately after verification.
- User-run IntelliJ execution may use the default `8080`.
- Push verified `main` changes to both `origin` and `cmc` while both repositories are maintained together.
- Do not commit real secrets, private keys, or runtime `.env` values.
- `docs/local-deploy-env.md` is intentionally ignored because it may describe local secret mappings.

## Verification

Code changes are complete only after both commands pass.

```bash
./gradlew test
./gradlew build
```

For API-facing changes, also check whether Swagger/OpenAPI needs updates.
For HTTP verification by Codex, use port `18080` only.

## Development Principles

- Make small, verifiable changes.
- Prefer one feature or one infrastructure concern per task.
- Do not guess domain policy.
- Add dependencies only for a clear documented reason.
- Do not update unrelated docs.
- Do not describe future plans as already implemented.
- Never commit secrets to the repository.

## Self Review Before Completion

Before reporting completion, Codex must review the change using the following checklist.

### Scope

- Did the change stay within the requested scope?
- Did it avoid implementing undocumented domain policy?
- Did it avoid adding unnecessary dependencies?
- Did it avoid modifying unrelated files?

### Code Quality

- Is the change small and verifiable?
- Is the logic understandable without excessive abstraction?
- Is there any obvious missing validation, transaction boundary, or side effect?
- Is there any over-engineering for the current MVP stage?

### Tests and Build

- Did `./gradlew test` pass?
- Did `./gradlew build` pass?
- If HTTP verification is relevant, was it checked?

### Documentation

- Were related docs updated only when actual code, setting, or policy changed?
- If docs were not updated, is that reasonable?
- Was Swagger/OpenAPI updated when API behavior changed?
- Was `docs/01-dbdiagram.md` updated when DB schema changed?
- Do not update unrelated docs.
- Do not describe future plans as already implemented.

### Report Format

After each task, report only:

1. Changed files
2. Summary
3. Verification result
4. Docs updated or not updated
5. Uncertain points
6. Intentionally not implemented items
