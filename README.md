# sample-jee8

Java EE 8 (`javax.*`) REST CRUD for `Book` — JAX-RS 2.1 + CDI 2.0 + JPA (EclipseLink) over an
in-memory H2 database, deployed on **Oracle WebLogic 14.1.2** / **JDK 21**.

- http://localhost:7001/sample-jee8
- http://localhost:7001/sample-jee8/api/test
- http://localhost:7001/sample-jee8/api/books
- http://localhost:7001/sample-jee8/api-docs.html — Swagger UI
- http://localhost:7001/sample-jee8/api/openapi.json / `.yaml`

## Build & run

```powershell
./mvnw.cmd clean install    # build + Spotless check + tests + coverage gate -> target/SampleJEE8-0.0.1-SNAPSHOT.war
.\mvn-clean-install.ps1     # same, plus auto-formats first (spotless:apply)
.\mvn-spotless-apply.ps1    # just reformat the code
.\autodeploy.ps1            # build, then drop the WAR into the WebLogic base_domain autodeploy folder
```

Requires JDK 21+. On this machine, Maven can't reach Maven Central through the
WebLogic-bundled JDK's truststore — until that's fixed, add these flags to prime `~/.m2`:

```
-Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```

## Books API

| Method | Path | |
|---|---|---|
| `POST` | `/api/books` | create (`201` + `Location`; `409` on duplicate ISBN) |
| `GET` | `/api/books` | list — `?title=&author=&genre=&page=0&size=20&sort=title,asc` |
| `GET` | `/api/books/{id}` | one (`404` if missing) |
| `PUT` | `/api/books/{id}` | full replace |
| `PATCH` | `/api/books/{id}` | partial update |
| `DELETE` | `/api/books/{id}` | delete (`204`) |

Errors are JSON `ApiError`: `{ timestamp, status, error, message, path, details[] }`.

## Quality gates (all enforced by `mvn install`)

| Tool | What |
|---|---|
| [Spotless](https://github.com/diffplug/spotless) + google-java-format | formatting check, runs in `process-sources` |
| [Lombok](https://projectlombok.org) | boilerplate (`@Getter`/`@Data`/`@Builder`/…) |
| [JUnit 5](https://junit.org/junit5/) + [Mockito](https://site.mockito.org/) | 31 tests: unit (mapper/service), real H2+EclipseLink (repository), JAX-RS layer (resource, exception mappers) |
| [JaCoCo](https://www.jacoco.org/jacoco/) | line coverage gate, minimum **70%**, runs in `verify` — report at `target/site/jacoco/index.html` |
| `.editorconfig` | matches the Spotless/google-java-format style |

## Project layout

```
book/        entity, repository, service, JAX-RS resource, mapper, DTOs
error/       ApiError + custom exceptions + JAX-RS ExceptionMappers
persistence/ EntityManager producer + @Tx interceptor (RESOURCE_LOCAL, app-managed)
test/        TestResource sanity check

openapi-configuration.yaml   scopes the Swagger scan to org.timsoft.api (see CLAUDE.md)
```

## More

- [CLAUDE.md](CLAUDE.md) — architecture, persistence setup, build details.
- [PLANO-CRUD-BOOKS.md](PLANO-CRUD-BOOKS.md) — the design plan this CRUD was built from.
