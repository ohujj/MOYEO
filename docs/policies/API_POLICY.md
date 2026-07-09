# API Policy

> Purpose: Canonical policy for Moyeo API success responses, error responses,
> and Swagger/OpenAPI documentation.

## Successful API Responses

API-001: Successful APIs return response DTOs directly without a common success
wrapper.

- Keep `/health`, Actuator, and Swagger/OpenAPI responses in their native
  formats.

## API Errors

ERR-001: API errors follow RFC 9457 Problem Details and expose a stable `code`
without leaking internal exception information.

- Express API errors with the `application/problem+json` media type.
- Add a stable `code` property for client-side branching.
- Add an `errors` property only for validation details.
- Do not expose rejected values, exception types, stack traces, or internal
  exception messages.
- Define domain error codes only after the related domain policy is documented.
- Current common error handling is temporary until domain-specific errors are
  defined.

## Swagger/OpenAPI Documentation

- Use Swagger/OpenAPI as the primary API contract reference.
- When adding or changing APIs, keep Swagger easy to read for frontend
  collaboration.
- Swagger/OpenAPI의 summary, description, DTO 필드 설명, 응답 설명, 예시는 사용자가
  별도로 요청하지 않는 한 한글로 작성한다.
- Add operation summaries, descriptions, DTO field descriptions, and example
  values when they help API consumers.
- For enum or code-like fields, document the allowed values and their meanings.
- For mode-dependent fields, document when each field is required, nullable,
  ignored, or returned as an empty list.
- For complex request flows, provide Swagger examples that frontend developers
  can copy and adjust.
- Until authentication/authorization is hardened or replaced by a different
  framework-level approach, controller parameters that inject the current user
  through `@CurrentMember AuthenticatedMember` must be hidden from Swagger with
  `@Parameter(hidden = true)`.
- For APIs that require the current user, show the Bearer token requirement in
  Swagger instead of exposing the internal current-user parameter.
- If the current-user injection structure changes later and Swagger no longer
  exposes these internal parameters, this temporary documentation rule and
  related annotations may be removed.
- Keep Notion focused on product flow, policies, decisions, and links to Swagger
  rather than duplicating every request/response shape.
