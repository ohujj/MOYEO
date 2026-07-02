# AGENTS.md

## Project

This is the CMC Moyeo six-week MVP backend server.

Priorities:
- Complete the core MVP flow.
- Keep the server deployable.
- Avoid premature infrastructure or domain over-engineering.

## Required Context

Before changing code, inspect the relevant files directly.

Use these documents as routing references:
- `docs/00-project-setup.md`: project decisions, excluded scope, API/error/auth/deployment policies.
- `docs/01-dbdiagram.md`: current DBML schema and table relationship reference.

For deployment or environment work:
- Check the actual committed deployment files first.
- Do not rely on local-only notes unless the user explicitly provides them.

## Hard Rules

- Do not invent undocumented domain policy.
- If domain policy is missing, leave a TODO/proposal instead of implementing behavior.
- Do not change authentication/security policy without explicit user direction.
- Do not change API response/error policy without explicit user direction.
- Do not change deployment architecture without explicit user direction.
- Do not expand the DB schema without explicit user direction.
- Do not add Redis, Kafka, WebSocket, NoSQL, Nginx, blue/green, or complex hooks unless explicitly requested.
- Do not commit or print real secrets.

## API Rules

- Successful API responses return response DTOs directly.
- API errors follow RFC 9457 Problem Details with `application/problem+json`.
- Error responses must include a stable `code`.
- Do not expose rejected values, exception types, stack traces, or internal exception messages.
- Keep `/health`, Actuator, and Swagger/OpenAPI responses in their native formats.

## Schema and Documentation

When JPA entities, table names, columns, indexes, unique constraints, or relationships change:
- Update `docs/01-dbdiagram.md`.
- Keep DBML notes concise and aligned with the code.

When APIs change:
- Keep Swagger/OpenAPI annotations useful for frontend collaboration.
- Hide internal current-user injection parameters from Swagger.

## Change Scope Checklist

For each change, classify the touched surface before editing:
- API contract: controller paths, methods, request/response DTOs, status codes, headers, or Swagger annotations.
- Error contract: error codes, HTTP status mapping, validation behavior, or Problem Details shape.
- Schema contract: JPA entities, table names, columns, indexes, unique constraints, or relationships.
- Auth/security contract: JWT behavior, current-user injection, CORS, password handling, or access rules.
- Deployment/runtime contract: Docker, GitHub Actions, profiles, environment variables, or external services.
- Domain policy: room, participant, voting, place, schedule, result, or member behavior not already documented.

If a change touches one of these surfaces, update the matching documentation or explicitly report why no update was needed.
If a requested behavior needs missing domain policy, leave a TODO/proposal instead of inventing the rule.

## Git Rules

- Use an English conventional commit prefix such as `feat:`, `fix:`, `docs:`, `chore:`, or `test:`.
- Write the commit subject/body content in Korean unless the user explicitly requests another language.

## Verification Before Completion

Before reporting completion:
- Run the relevant build/test command.
- Report the exact commands executed and their results.
- Report changed files.
- Report whether Swagger/OpenAPI was updated or not needed.
- Report whether DBML was updated or not needed.
- Report any unresolved policy uncertainty.
