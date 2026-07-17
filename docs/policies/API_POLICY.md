# API Policy

> Purpose: Canonical policy for Moyeo API success responses, error responses,
> and Swagger/OpenAPI documentation.

## Successful API Responses

API-001: Successful APIs return response DTOs directly without a common success
wrapper.

- Keep `/health`, Actuator, and Swagger/OpenAPI responses in their native
  formats.

## Request Trace ID

API-002: The server generates a new trace ID for every HTTP request and returns
it in the `X-Trace-Id` response header.

- The same value is written to application and exception logs for that request.
- Clients may provide the header when reporting an issue, but the server does
  not trust or reuse a client-supplied trace ID.
- The error response body remains unchanged; use the response header to
  correlate it with logs.

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

- Swagger/OpenAPI의 operation summary, description, DTO 필드 설명, 응답 설명,
  예시 객체 이름과 설명은 사용자가 명시적으로 다른 언어를 요청하지 않는 한
  한글로 작성한다.
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

### Enum and mode-dependent request examples

When a request has multiple valid enum-driven flows, provide one named Swagger
example for each supported flow. Keep the JSON itself executable: do not insert
`//` or block comments into the example body.

Put the following information in each `@ExampleObject` description using
Markdown headings and bullets for readability:

- All allowed values of the flow-selecting enum, and which value the example
  represents.
- Fields required by that flow.
- Fields that must be omitted or are not used by that flow.
- Allowed values and conditional requirements of related enums.
- Values derived by the server that clients must not send.

Example names should use the actual enum value, such as `SCHEDULE_ONLY`, so a
consumer can select a valid flow without translating a display label. This
format is especially useful when Swagger must serve as a copy-ready reference
for frontend collaboration.
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
