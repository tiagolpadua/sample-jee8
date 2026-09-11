# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A small Java EE 8 (`javax:javaee-api` 8.0.1, `provided` scope) web app packaged as a WAR: a
**REST CRUD for `Book`** backed by **JPA + an in-memory H2 database**, plus a trivial
`TestResource` sanity check and an OpenAPI/Swagger UI. Target runtime is **Oracle WebLogic 14.1.2**.

The code uses the pre-Jakarta `javax.*` namespace throughout (`javax.ws.rs.*`,
`javax.enterprise.context.*`, `javax.persistence.*`) and the Java EE 8 XML descriptor namespace
`http://xmlns.jcp.org/xml/ns/javaee`. (Jakarta EE 9 later renamed every `javax.*` EE package to
`jakarta.*`; this project predates that.) The API surface is Java EE 8, but the code is
**compiled for JDK 21** (`maven.compiler.release` = `21`) — WebLogic 14.1.2 runs on JDK 21.

## Build & run

- Build the WAR: `./mvnw.cmd clean install` (or `mvn-clean-install.ps1`) → produces
  `target/SampleJEE8-0.0.1-SNAPSHOT.war`. Requires JDK 21+. Runs Spotless + the JUnit 5 tests.
- Deploy to WebLogic: `autodeploy.ps1` — sets `JAVA_HOME` to the WebLogic-bundled JDK 21, builds,
  then drops the WAR into
  `C:\KDI\wl_14.1.2.0.0\Oracle\Middleware\Oracle_Home\user_projects\domains\base_domain\autodeploy`.
- App is served at `http://localhost:7001/sample-jee8` (default WebLogic port).
- **Maven + this JDK can't reach Maven Central** over the default resolver transport (the
  WebLogic-bundled JDK's truststore rejects Central's TLS cert). Until the truststore is fixed,
  prime the local `~/.m2` with:
  `./mvnw.cmd ... -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true`

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/sample-jee8/` | static `index.html` |
| `GET` | `/sample-jee8/api/test` | `TestResource` → `"Test OK"` |
| `GET` | `/sample-jee8/api/openapi.json` / `.yaml` | OpenAPI 3 doc (scan scoped — see below) |
| `GET` | `/sample-jee8/api-docs.html` | Swagger UI |
| `POST` | `/sample-jee8/api/books` | create → `201` + `Location`; `409` on duplicate ISBN |
| `GET` | `/sample-jee8/api/books` | paged list; query `title`, `author`, `genre`, `page`, `size`, `sort=field,asc\|desc` |
| `GET` | `/sample-jee8/api/books/{id}` | `200` / `404` |
| `PUT` | `/sample-jee8/api/books/{id}` | full replace |
| `PATCH` | `/sample-jee8/api/books/{id}` | partial update (custom `@PATCH` verb) |
| `DELETE` | `/sample-jee8/api/books/{id}` | `204` / `404` |

Path composition: context root `/sample-jee8` (from `WEB-INF/weblogic.xml`) +
`@ApplicationPath("api")` on `ApplicationConfig` + `@Path` on each resource.

## Architecture

Layers, all in `org.timsoft.api`:

- `book.BookResource` — JAX-RS, `@RequestScoped`, DTO in / DTO out, `@Valid` on bodies.
- `book.BookService` — rules (`@ApplicationScoped`); writes annotated `@Tx`.
- `book.BookRepository` — `EntityManager` access; dynamic filter/paging via Criteria API.
- `book.BookMapper` — entity ⇄ `BookRequest` / `BookPatchRequest` / `BookResponse` (manual).
- `book.dto.*` — request/response DTOs (Lombok) + generic `PageResponse<T>`.
- `error.*` — `ApiError` body + custom exceptions + 5 `ExceptionMapper`s (404/409/400/409/500),
  registered explicitly in `ApplicationConfig.getClasses()`.
- `persistence.*` — `EntityManagerProducer` (`@Produces` an `@RequestScoped`, application-managed
  `EntityManager` from a `RESOURCE_LOCAL` unit) and `TransactionalInterceptor` bound by `@Tx`
  (self-enabled via `@Priority`, no `beans.xml` entry).
- OpenAPI doc: the **stock** `io.swagger.v3.jaxrs2.integration.resources.OpenApiResource`,
  registered in `ApplicationConfig.getClasses()` like any other resource — no custom code. Its
  scan is scoped by `src/main/resources/openapi-configuration.yaml`
  (`resourcePackages: [org.timsoft.api]`), auto-discovered from the classpath the first time the
  resource runs. **Without `resourcePackages`, swagger-core's ClassGraph scanner walks the whole
  WebLogic classpath and runs the server out of heap** — this file is not optional.

Other notes:

- **JAX-RS is activated with no `web.xml` servlet mapping** — `ApplicationConfig extends
  Application` with `@ApplicationPath` is the only registration; `web.xml` (Servlet 4.0) just
  declares the welcome file.
- **CDI 2.0** via `WEB-INF/beans.xml` (`bean-discovery-mode="annotated"`) — every bean carries an
  explicit scope.
- **Artifact name vs. context root** — WAR is `SampleJEE8-*`, context root is `/sample-jee8` (set
  in `WEB-INF/weblogic.xml`). Change one → update the other + `README.md`.

## Persistence

- **PU `sampledbPU`**, `RESOURCE_LOCAL`, provider EclipseLink (supplied by WebLogic at runtime —
  **not** bundled). `src/main/resources/META-INF/persistence.xml` is the production unit; it uses
  JPA schema generation (`drop-and-create`) and seeds 4 rows from `META-INF/sql/books-seed.sql`.
- **H2 2.3.x** driver **is** bundled (`com.h2database:h2`, `compile`). URL
  `jdbc:h2:mem:sampledb;DB_CLOSE_DELAY=-1;…;MODE=LEGACY`.
  - `DB_CLOSE_DELAY=-1` keeps the in-memory DB alive between connections (required).
  - `MODE=LEGACY` — EclipseLink 2.7's H2 dialect emits the H2 1.x `BIGINT IDENTITY` column type,
    which H2 2.x only accepts in legacy mode.
  - Timestamps are `LocalDateTime`, not `Instant` — EclipseLink 2.7 maps `Instant` to a binary blob.
  - `@Version` starts at **1** on insert (EclipseLink convention, not 0).
- Data resets on every redeploy/restart (in-memory + `drop-and-create`).
- `src/test/resources/META-INF/persistence.xml` shadows the production unit for tests: same PU
  name, a separate `testdb`, no seed script — repository tests start empty.

## Tests

`mvn test` (JUnit 5), no container:

- `book.BookMapperTest` — pure unit.
- `book.BookServiceTest` — Mockito over the repository/mapper.
- `book.BookRepositoryTest` — real H2 + EclipseLink (`org.eclipse.persistence:eclipselink:2.7.15`,
  `test` scope; 2.7.x is the last `javax` line).
