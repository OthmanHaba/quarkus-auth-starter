# Authentication implementation note

- **Quarkus:** 3.38.1, Kotlin 2.4.0, Java 21, Gradle Kotlin DSL.
- **Persistence:** blocking Hibernate ORM with Panache Kotlin repositories and PostgreSQL.
- **Migrations:** Flyway SQL migrations under `src/main/resources/db/migration`.
- **User model:** `com.example.starter.domain.user.UserEntity`; generated PostgreSQL `BIGSERIAL` represented as Kotlin `Long`.
- **Roles:** `UserRole` enum (`CUSTOMER`, `ADMIN`) persisted as a string.
- **Existing authentication:** locally issued RSA JWT bearer tokens through SmallRye JWT, bcrypt password verification, a request-scoped `AuthFacade`, and a static `Auth` ArC lookup. Domain authorization uses the CDI `@Gate(Ability)` interceptor.
- **Existing errors:** localized `ApiException` responses selected from `Accept-Language`, currently English and Arabic.
- **Database/testing:** PostgreSQL 17 through Compose in development and Quarkus Dev Services in tests.

## Package boundaries

- `com.example.starter.http` owns REST resources, request/response DTOs, filters,
  localization, and error mapping.
- `com.example.starter.application` owns actions and public authentication and
  authorization contracts.
- `com.example.starter.domain` currently owns only the user entity and repository;
  new starter applications add their own domain packages here.
- `com.example.starter.internal` owns the Quarkus-specific authentication implementation and
  CDI authorization interceptors.
- `com.example.starter.shared` contains only framework-independent error concepts.

## Existing files modified

- `build.gradle.kts` and `application.properties` for security, CORS, and authentication configuration.
- Existing auth DTO/resource/action code to replace JWT login with session login while preserving the already-existing registration endpoint.
- `GateInterceptor` to use injected `CurrentAuth` instead of static CDI access.
- Tests, test database reset support, README, and localized message bundles.

## Conflicts and adaptation decisions

1. Opaque personal tokens and SmallRye JWT cannot safely own the same Bearer header. The local JWT issuer/verifier is removed and the opaque-credential mechanism becomes the application authentication mechanism.
2. The specification forbids static global CDI access. The existing `Auth` object and `AuthFacade` are replaced by injected, request-scoped `CurrentAuth`.
3. Registration already exists, so it remains available but no longer issues a JWT. Login rotates a server-side session and returns user/session metadata.
4. User and token IDs are `Long`; session IDs are UUID.
5. Token abilities and application `@Gate` authorization remain separate. Token annotations check credential scope; Gate checks domain roles/rules.
6. Trusted SPA session authentication requires an exact configured `Origin` or origin derived from `Referer`. Missing/untrusted origins never authenticate cookies.
7. An invalid session cookie on a trusted request is a terminal authentication failure and never falls back to Bearer authentication.
8. Blocking Hibernate validation runs through `AuthenticationRequestContext.runBlocking` in identity providers.
9. Method-level token-ability annotations override class-level annotations of the same kind; when only class-level annotations exist, they apply to every intercepted method.
10. Development uses an insecure (`Secure=false`) cookie override; production defaults to secure cookies.

## Auth-starter conversion

The earlier product, checkout, and order examples were removed. The starter now
contains only users, authentication, token/session management, authorization, and
localized errors. Flyway was intentionally reset to two clean migrations:
`V1__create_users.sql` and `V2__create_auth_credentials.sql`. Existing local shop
database volumes must be recreated before using this starter history.
