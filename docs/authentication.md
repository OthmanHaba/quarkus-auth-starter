# Authentication

## Purpose and architecture

This application contains a Laravel Sanctum-inspired authentication implementation
under `com.example.starter.internal.auth`. Its public contracts live under
`com.example.starter.application.auth`. It is application code, not a Quarkus
extension or separate service. It supports trusted first-party SPA sessions and
opaque personal access tokens through one Quarkus `SecurityIdentity` integration.

```text
Request
  -> trusted Origin/Referer + session cookie -> session identity
  -> otherwise Bearer <token-id>|<secret>    -> personal-token identity
  -> otherwise                               -> anonymous identity
```

For a trusted request that contains a session cookie, an invalid, expired, revoked,
or malformed session is a terminal authentication failure. The mechanism never
falls back to a bearer token on that request. With no session cookie, bearer
authentication remains available regardless of browser origin.

Blocking Hibernate validation is delegated by the identity providers through
Quarkus's blocking authentication request context. The resulting identity contains
the principal, application roles, credential permissions, and typed auth
attributes. Application code reads those through the request-scoped `CurrentAuth`
API instead of using string attributes or a static CDI lookup.

## Trusted SPA origins

`StatefulOriginMatcher` parses and compares the scheme, host, and effective port.
It prefers `Origin` and otherwise considers `Referer`. Missing or malformed origins
are not trusted. Substrings and implicit subdomains are never accepted. A session
cookie from a non-trusted request is ignored.

Configure every permitted frontend explicitly. Credentialed CORS uses the same
exact origins and never uses `*`.

## Credential formats and storage

Personal tokens have this one-time plaintext form:

```text
<numeric-token-id>|<256-bit-random-secret>
```

Sessions have this cookie value:

```text
<uuid-session-id>|<256-bit-random-secret>
```

Secrets come from `SecureRandom` and URL-safe Base64 without padding. PostgreSQL
stores only each secret's SHA-256 hash. Authentication loads by the public ID and
uses `MessageDigest.isEqual` for constant-time hash comparison. Passwords remain
bcrypt hashes. Plaintext credentials and hashes are never returned by list APIs or
written to logs.

The `personal_access_tokens` table stores owner, safe name, JSONB abilities,
last-use time, expiry, revocation, and creation time. The `auth_sessions` table
stores its UUID, owner, secret hash, safe client metadata, last-use time, expiry,
revocation, and creation time. Both have owner, expiry, and partial active-owner
indexes. Foreign keys cascade when a user is deleted.

`last_used_at` is updated only after the configured throttle interval. A scheduled
hourly job removes expired credentials and revoked credentials older than the
configured retention period.

## Configuration

```properties
app.auth.stateful-origins=http://localhost:3000,https://app.example.com
app.auth.session-cookie-name=app_session
app.auth.csrf-cookie-name=XSRF-TOKEN
app.auth.session-duration=PT8H
app.auth.token-default-expiration=P30D
app.auth.last-used-update-interval=PT5M
app.auth.revoked-retention=P30D
app.auth.cookie-secure=true
app.auth.cookie-same-site=LAX
# app.auth.cookie-domain=example.com
```

Production cookies default to `HttpOnly` for sessions, `Secure`, `SameSite=Lax`,
and `Path=/`. The CSRF cookie is deliberately readable. Development and test
profiles disable `Secure` so local HTTP clients can use the cookie.

CORS is configured with the explicit frontend origins, credentials enabled, the
used request headers, and the supported HTTP methods. Keep the CORS and `app.auth`
origin lists aligned.

## Public developer API

Inject `PersonalTokens` to manage personal access tokens:

```kotlin
val token = personalTokens.createToken(
    userId = user.id,
    name = "mobile-app",
    abilities = setOf("profile:read", "profile:update"),
)

// Return token.plainTextToken only from this creation response.
personalTokens.tokens(user.id)
personalTokens.findToken(user.id, token.accessToken.id)
personalTokens.revokeToken(user.id, token.accessToken.id)
personalTokens.revokeOtherTokens(user.id, token.accessToken.id)
personalTokens.revokeAllTokens(user.id)
```

Inject `CurrentAuth` in request-handling application code:

```kotlin
currentAuth.check()
currentAuth.guest()
currentAuth.user()
currentAuth.userId()
currentAuth.type()
currentAuth.currentToken()
currentAuth.currentSession()
currentAuth.tokenCan("profile:update")
currentAuth.tokenCannot("users:manage")
currentAuth.tokenCanAny("profile:read", "profile:update")
currentAuth.tokenCanAll("profile:read", "profile:update")
```

`user()` and `userId()` throw the localized application `401` error for a guest.
Session identities pass token-ability checks, matching Laravel Sanctum's first-party SPA
semantics, but still must pass application `@Gate` role/domain authorization.

## Protecting endpoints and actions

`@TokenAbilities` requires every named ability. `@TokenAbilityAny` requires at
least one. Anonymous callers receive `401`; authenticated personal tokens missing
scope receive `403`; sessions pass. Both annotations work on CDI classes and
methods. A method annotation of the same kind overrides its class annotation.

```kotlin
@POST
@TokenAbilities("profile:update")
fun updateProfile(request: UpdateProfileRequest): ProfileResponse =
    updateProfileAction.execute(currentAuth.userId(), request.toCommand())
```

Quarkus-native permissions are also supported because personal-token abilities are
added to `SecurityIdentity`, while session identities supply a permission checker:

```kotlin
@GET
@PermissionsAllowed("sessions:read")
fun listSessions(): List<AuthSession> = sessionManager.sessions(currentAuth.userId())
```

Token scope answers whether a credential may request an operation. `@Gate`, roles,
and domain rules independently answer whether the user may perform it.

## HTTP integration

The integration surface is:

```text
POST   /api/auth/login
POST   /api/auth/register
POST   /api/auth/logout
GET    /api/auth/me
GET    /api/auth/csrf-cookie
GET    /api/auth/tokens
POST   /api/auth/tokens
DELETE /api/auth/tokens/{tokenId}
GET    /api/auth/sessions
DELETE /api/auth/sessions/{sessionId}
DELETE /api/auth/sessions/others
GET    /api/users
```

Login requires a trusted origin, verifies the existing bcrypt password, rotates an
existing authenticated session, creates a server-side session, and sets the
session cookie. Session logout revokes the current session and expires its cookie.
Bearer logout revokes the current personal token. Management operations are scoped
to the current owner, so another user's ID is reported as not found.

Registration was already part of this application and remains available. It does
not issue any credential.

### SPA example

```bash
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Origin: http://localhost:3000' \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"password123"}'

curl -i -b cookies.txt -c cookies.txt http://localhost:8080/api/auth/csrf-cookie \
  -H 'Origin: http://localhost:3000'

# Read XSRF-TOKEN from the cookie jar and send the same decoded value as X-XSRF-TOKEN.
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/tokens \
  -H 'Origin: http://localhost:3000' \
  -H 'X-XSRF-TOKEN: <csrf-cookie-value>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"browser-created","abilities":["profile:read"]}'
```

### Personal token example

```bash
curl http://localhost:8080/api/auth/me \
  -H 'Authorization: Bearer 25|<one-time-secret>'
```

## CSRF behavior

The focused double-submit filter protects `POST`, `PUT`, `PATCH`, and `DELETE`
requests authenticated by a session. The `X-XSRF-TOKEN` header must match the
readable `XSRF-TOKEN` cookie through a constant-time comparison. Safe methods and
bearer-authenticated requests skip CSRF validation. Do not turn this off to work
around a frontend integration error.

## Security limitations

- This package does not implement OAuth, refresh tokens, email verification,
  password reset, or account registration policy.
- `CurrentAuth` is request-scoped; jobs should receive an actor ID explicitly.
- Origin trust and CORS configuration must be updated together for each deployment.
- A stolen bearer token or session cookie remains usable until expiry/revocation;
  use TLS, short appropriate lifetimes, and client-side cookie protections.
- Token and session listing includes safe metadata such as IP and user agent; apply
  the application's privacy and retention policy to those values.

## Testing

Docker must be running. Quarkus Dev Services starts isolated PostgreSQL:

```bash
./gradlew test
./gradlew build
```

`TokenAuthTest` covers hash-only storage, token parsing and validation, expiry,
revocation, inactive/deleted owners, abilities, native permissions, ownership,
trusted sessions, exact origins, session priority, invalid-session no-fallback,
CSRF, CORS, rotation, logout, roles, and throttled last-use updates.
