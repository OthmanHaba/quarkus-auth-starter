# Quarkus Kotlin Auth Starter

An auth-first starter for new Quarkus applications. It includes Kotlin, PostgreSQL,
Flyway, Hibernate ORM Panache, localized API errors, server-side SPA sessions,
opaque personal access tokens, token abilities, and application authorization with
`@Gate`.

There are no sample product, checkout, or order domains. Add the first application
domain beside the existing auth foundation when starting a project.

<!-- starter-only:start -->
## Start a new application

One command, no checkout needed:

```bash
curl -fsSL https://raw.githubusercontent.com/OthmanHaba/quarkus-auth-starter/main/create-app.sh | bash -s -- ~/sites/shop com.acme.shop
```

The two arguments are the target directory and the base package. From a local
clone, run `./create-app.sh ~/sites/shop com.acme.shop` instead. Both are
prompted for if omitted.

The script copies the tree and rewrites every identifier that carries the
starter's name:

| Renamed | From | To |
| --- | --- | --- |
| Kotlin package (all source roots, directories included) | `com.example.starter` | `com.acme.shop` |
| Gradle group | `com.example` | `com.acme` |
| `rootProject.name` | `quarkus-auth-starter` | `shop` |
| Postgres database, user, password, volume | `auth_starter` | `shop` |
| Docker image tags in the sample Dockerfiles | `quarkus/auth-starter` | `quarkus/shop` |

It then runs `git init` and makes the first commit. `LICENSE` is not copied, so
the new project picks its own. Nothing is written back into the starter.

Requirements: `git`, `bash`, `perl`, `tar` — all present by default on macOS and
Linux.

<!-- starter-only:end -->
## Structure

```text
com.example.starter/
├── http/
│   ├── auth/           auth, token and session endpoints
│   ├── user/           admin-protected user endpoint
│   ├── filter/         session CSRF protection
│   ├── error/          localized HTTP exception mapping
│   └── localization/   request locale resolution
├── application/
│   ├── action/auth/    login and registration use cases
│   ├── auth/           CurrentAuth, PersonalTokens and SessionManager contracts
│   └── authorization/  Ability, @Gate and token-ability annotations
├── domain/user/        user entity and repository
├── internal/
│   ├── auth/           Quarkus Security, tokens, sessions, cookies and cleanup
│   └── authorization/  Gate and token-ability interceptors
└── shared/             framework-independent API errors
```

HTTP requests are converted into application commands before invoking actions.
Application and domain code do not import HTTP DTOs. Straightforward reads may stay
in resources; create a Query class only when a read becomes complex.

## Included authentication

- Trusted first-party SPA authentication using opaque HttpOnly session cookies.
- Opaque bearer tokens in `<id>|<secret>` format.
- SHA-256 secret hashes; plaintext credentials are never stored.
- `CurrentAuth` for the current user, authentication type, token and session.
- `@TokenAbilities`, `@TokenAbilityAny`, and Quarkus `@PermissionsAllowed`.
- `@Gate(Ability)` for application roles and domain authorization.
- Double-submit CSRF protection for session-authenticated mutations.
- Exact-origin credentialed CORS.
- Credential expiry, revocation, last-use throttling and scheduled cleanup.
- English and Arabic error localization.

See [the authentication guide](docs/authentication.md) for architecture,
configuration, security decisions, and client examples.

## Run locally

Requirements are JDK 21 and Docker.

```bash
docker compose up -d
./gradlew quarkusDev
```

The API runs at `http://localhost:8080`. The default trusted development frontend
origin is `http://localhost:3000`.

## Auth API

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me
GET    /api/auth/csrf-cookie

GET    /api/auth/tokens
POST   /api/auth/tokens
DELETE /api/auth/tokens/{id}

GET    /api/auth/sessions
DELETE /api/auth/sessions/{id}
DELETE /api/auth/sessions/others

GET    /api/users
```

Registration creates a `CUSTOMER` but does not issue a credential. Browser login
requires a configured origin and creates a server-side session. Personal-token
plaintext is returned once, only when the token is created.

Auth management uses starter abilities:

```text
tokens:read
tokens:manage
tokens:create
tokens:revoke
sessions:read
sessions:revoke
```

The wildcard ability `*` satisfies every **token ability** check — both the
`@TokenAbilities` / `@TokenAbilityAny` interceptors and Quarkus
`@PermissionsAllowed`. It is matched dynamically in `PersonalAccessToken.can()`,
so it also covers abilities you add later without reissuing tokens.

`*` does **not** grant `@Gate(...)`. Gates authorize on the user's application
role, not on the credential, so a `*` token acting for a `CUSTOMER` is still
denied `@Gate(Ability.MANAGE_USERS)`. That split is the point: a wildcard token
says "this credential is unrestricted", never "this user is an admin".

Session-authenticated requests pass every token-ability check (there is no token
to scope) but still pass through application Gates.

## Extending Gate

Add a named ability:

```kotlin
enum class Ability {
    MANAGE_USERS,
    PUBLISH_ARTICLES,
}
```

Add its application rule in `GateInterceptor`, then annotate a resource, action, or
other CDI method:

```kotlin
@Gate(Ability.PUBLISH_ARTICLES)
fun publish(command: PublishArticleCommand) = ...
```

Anonymous requests receive `401`; authenticated users denied by the rule receive
`403`.

## Test and build

Tests use Quarkus Dev Services with isolated PostgreSQL:

```bash
./gradlew test
./gradlew build
```

Run the packaged application with:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

## Documentation

- [Authentication guide](docs/authentication.md) — architecture, trusted origins,
  credential formats and storage, every `app.auth.*` setting, CSRF and CORS,
  the `PersonalTokens` and `CurrentAuth` APIs, and client examples.
- [Implementation note](docs/authentication-implementation.md) — why the design
  landed where it did, and the conflicts resolved along the way.

## License

MIT. See [LICENSE](LICENSE).
