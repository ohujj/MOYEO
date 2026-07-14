# Authentication Policy

> Purpose: Canonical policy for Moyeo service identity, login identities, JWT,
> CORS, and authentication-related security boundaries.

## Identity Model

AUTH-001: Service identity, local credential identity, and social provider
identity remain separated through `User`, `LoginAccount`, and `SocialAccount`.

- Keep `User` as the service user identity.
- Keep local login credentials in `LoginAccount` instead of storing password data
  directly on `User`.
- Keep social provider identities in `SocialAccount` using
  `provider + providerUserId`.
- `providerUserId` is the provider-issued user identifier, not CI/DI.
- Do not store CI/DI unless a separate human decision, consent policy, and
  security policy are documented.

## Access JWT

- Use an Access JWT for local signup/login responses and protected API
  authentication.
- Validate Access JWT format, signature, required headers and claims, expiration,
  and required JWT configuration at startup.
- Keep the current JWT implementation minimal: no refresh token, logout, token
  rotation, or guest token yet.
- Store real JWT secrets through environment variables in dev/prod.

## Development Test Accounts

AUTH-002: The `local` and `dev` profiles may seed a fixed, idempotent pair of
local test accounts to support frontend development. This initialization must
not run in the `prod` profile.

- The test-account token endpoint is available only when the `local` or `dev`
  profile is active.
- It may issue Access JWTs without a password only for the two fixed test
  accounts, and returns both accounts in one response.
- The endpoint is a temporary development convenience and must not be used as
  a production authentication mechanism.

## CORS

- Configure CORS with explicit frontend origins and update them when frontend
  deployment URLs are decided.
- If `CORS_ALLOWED_ORIGINS` exists in the EC2 runtime `.env`, it overrides the
  default origins in `application-dev.yml`.
