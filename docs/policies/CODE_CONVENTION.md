# Code Convention

> Purpose: Keep current Java package names and Swagger/OpenAPI descriptions
> consistent for frontend collaboration.

## Java Naming

- Package by technical layer and domain: `controller.<domain>`,
  `service.<domain>`, `domain.<domain>`, and `repository.<domain>`.
- A domain-owned external API integration may use `com.moyeo.<domain>` when it
  contains only that provider's configuration, client service, and private
  provider DTOs; controllers remain under `controller.<domain>`.
- Use PascalCase for classes, records, and enums; use camelCase for methods,
  fields, and request/response JSON properties.
- Name request and response DTOs with the operation and role, such as
  `AddressSearchRequest` and `AddressSearchResponse`.
- Keep provider-specific DTOs private to the service that calls the provider.

## Swagger/OpenAPI

- Write summaries, descriptions, DTO field descriptions, response
  descriptions, and examples in Korean unless another language is explicitly
  requested.
- Use a concise verb-noun summary, such as `도로명주소 검색`.
- State authentication, temporary behavior, and nullable or omitted fields in
  the operation description when they affect frontend behavior.
- Declare the normal success response and each documented error response with
  its HTTP status and error code example.
- Provide a copyable JSON request example for request bodies.
