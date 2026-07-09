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

## CORS

- Configure CORS with explicit frontend origins and update them when frontend
  deployment URLs are decided.
- If `CORS_ALLOWED_ORIGINS` exists in the EC2 runtime `.env`, it overrides the
  default origins in `application-dev.yml`.
