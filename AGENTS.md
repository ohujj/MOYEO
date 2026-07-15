# AGENTS.md

## Project Context

Moyeo is a product intended for continued development and operation. The current
six-week period is the initial MVP phase and first delivery milestone, not the
expected lifetime of the project.

Current priorities:

- Complete the core MVP flow.
- Keep the server deployable and maintainable.
- Prefer the simplest design that satisfies the current confirmed requirement.
- Avoid infrastructure or abstractions built only for hypothetical future scale.
- Avoid disposable MVP shortcuts that create unnecessary schema, API, migration,
  or operational debt.
- Preserve room for incremental evolution when requirements are confirmed.

## Working Rule

Before changing code or documentation:

1. Inspect the relevant files directly.
2. Classify the change surface.
3. Read the applicable canonical policy document.
4. Identify missing product/domain policy before implementation.
5. Confirm the expected impact before editing.
6. After implementation, verify build/test results and documentation
   synchronization.

## Routing References

- Project lifecycle, MVP scope, tech decisions, deployment:
  `docs/00-project-setup.md`
- API and error contract: `docs/policies/API_POLICY.md`
- Auth and security: `docs/policies/AUTH_POLICY.md`
- Meeting, participant, invite, and schedule availability domain:
  `docs/policies/MEETING_PARTICIPATION_POLICY.md`
- Schema contract: `docs/01-dbdiagram.md`
- AI code review: `docs/ai/CODE_REVIEW.md`

For deployment or environment work:

- Check the actual committed deployment files first.
- Do not rely on local-only notes unless the user explicitly provides them.

## Hard Rules

- Do not invent undocumented product or domain policy.
- If domain policy is missing, leave a TODO/proposal or report
  `POLICY_UNDEFINED` instead of implementing behavior as policy.
- Do not change authentication/security policy without explicit user direction.
- Do not change API response/error policy without explicit user direction.
- Do not change deployment architecture without explicit user direction.
- Do not commit or print real secrets.
- Do not introduce infrastructure for hypothetical scale or unconfirmed future
  requirements.
- Do not introduce disposable dev-only architecture as if it were a permanent
  production decision.
- Temporary MVP decisions must remain identifiable as temporary when they create
  later migration or operational work.
- Redis, Kafka, WebSocket, NoSQL, Nginx, blue/green deployment, complex hooks,
  MCP, and sub-agents remain examples of infrastructure to avoid unless a
  concrete need and explicit direction exist.

## Schema Change Rule

- Do not expand the schema solely for speculative future needs.
- If an explicitly requested feature requires a schema change, explain the
  required schema change before editing.
- Do not introduce unrelated tables, columns, indexes, unique constraints, or
  relationships without explicit approval.
- Avoid disposable schema shortcuts when they conflict with confirmed product
  direction.
- A future possibility alone is not enough reason to redesign the schema.

When JPA entities, table names, columns, indexes, unique constraints, or
relationships change:

- Read `docs/01-dbdiagram.md`.
- Update DBML and notes in `docs/01-dbdiagram.md`.
- Keep DBML notes concise and aligned with the code.

## Change Surface Checklist

For each change, classify the touched surface before editing:

- API contract: controller paths, methods, request/response DTOs, status codes,
  headers, or Swagger annotations.
- Error contract: error codes, HTTP status mapping, validation behavior, or
  Problem Details shape.
- Schema contract: JPA entities, table names, columns, indexes, unique
  constraints, or relationships.
- Auth/security contract: JWT behavior, current-user injection, CORS, password
  handling, or access rules.
- Deployment/runtime contract: Docker, GitHub Actions, profiles, environment
  variables, or external services.
- Domain policy: meeting, participant, voting, place, schedule, result, or member
  behavior not already documented.

If a change touches one of these surfaces, update the matching documentation or
explicitly report why no update was needed.

## Documentation Synchronization

- API or error changes must follow `docs/policies/API_POLICY.md` and check
  Swagger/OpenAPI update needs.
- Auth/security changes must follow `docs/policies/AUTH_POLICY.md`.
- Meeting, participant, invite, or schedule availability changes must follow
  `docs/policies/MEETING_PARTICIPATION_POLICY.md`.
- Schema changes must update `docs/01-dbdiagram.md`.
- Deployment/runtime changes must be checked against
  `docs/00-project-setup.md` and the committed deployment files.

## Git Rules

- Before creating a commit, run a pre-commit self-review using
  `docs/ai/CODE_REVIEW.md`.
- Before pushing, inspect configured remotes. While the personal and CMC
  repositories are maintained together, push verified `main` changes to both
  `origin` and `cmc` as documented in `docs/00-project-setup.md`.
- Use an English conventional commit prefix such as `feat:`, `fix:`, `docs:`,
  `chore:`, or `test:`.
- Write the commit subject/body content in Korean unless the user explicitly
  requests another language.

## Verification Before Completion

Before reporting completion:

- Run the relevant build/test command.
- Report the exact commands executed and their results.
- Report changed files.
- Report whether Swagger/OpenAPI was updated or not needed.
- Report whether DBML was updated or not needed.
- Report any unresolved policy uncertainty.
