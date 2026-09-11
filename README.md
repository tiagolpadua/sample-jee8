# sample-jee8

Java EE 8 (`javax.*`) REST CRUD for `Book` — JAX-RS 2.1 + JPA (EclipseLink) + in-memory H2,
deployed on Oracle WebLogic 14.1.2.

- http://localhost:7001/sample-jee8
- http://localhost:7001/sample-jee8/api/test
- http://localhost:7001/sample-jee8/api/books
- http://localhost:7001/sample-jee8/api-docs.html — Swagger UI
- http://localhost:7001/sample-jee8/api/openapi.json

## Books API

| Method | Path | |
|---|---|---|
| `POST` | `/api/books` | create (`201` + `Location`) |
| `GET` | `/api/books` | list — `?title=&author=&genre=&page=0&size=20&sort=title,asc` |
| `GET` | `/api/books/{id}` | one |
| `PUT` | `/api/books/{id}` | replace |
| `PATCH` | `/api/books/{id}` | partial update |
| `DELETE` | `/api/books/{id}` | delete (`204`) |

Build: `./mvnw.cmd clean install` · Deploy: `autodeploy.ps1`. See `CLAUDE.md` for details
(including the Maven Central TLS workaround for this machine).
