# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Java EE 8 (`javax:javaee-api` 8.0.1, `provided` scope) skeleton web app packaged as a WAR. It exposes a single JAX-RS resource and exists as a starting point / deployment sanity check across multiple application servers (Open Liberty, GlassFish, WebLogic, WildFly/JBoss). There is no persistence, no business logic, and no test code (`src/test` holds only `.gitkeep`).

The code uses the pre-Jakarta `javax.*` namespace throughout (`javax.ws.rs.*`, `javax.enterprise.context.*`) and the Java EE 8 XML descriptor namespace `http://xmlns.jcp.org/xml/ns/javaee`. (Jakarta EE 9 later renamed every `javax.*` EE package to `jakarta.*`; this project predates that.) The API surface is Java EE 8, but the code is **compiled for JDK 21** (`maven.compiler.release` = `21`), so the runtime must be a Java EE 8 server that itself runs on JDK 21 — see below.

## Build & run

- Build the WAR: `mvn clean install` → produces `target/SampleJEE8-0.0.1-SNAPSHOT.war`.
- The helper scripts (`mvn-clean-install.sh`, `autodeploy.sh`) hardcode a macOS `JAVA_HOME` for a JDK 8 install and a GlassFish `autodeploy` path — they are machine-specific to the original author and will not work as-is elsewhere. Treat them as examples, not entry points.
- Run in Docker (Open Liberty on JDK 21): `./run.sh` — runs `mvn clean install` then `docker-compose up --no-deps --build`. The `Dockerfile` starts from `eclipse-temurin:21-jre-jammy`, downloads `openliberty-runtime` from Maven Central, `server create defaultServer`, drops in `src/main/liberty/config/server.xml` (enables `jaxrs-2.1` + `cdi-2.0`, HTTP on 8080) and `target/Sample*.war` (into the server's `dropins/`). The Maven build must run first. App is served at `http://localhost:8080/sample-jee8`. Verified: `GET /sample-jee8/api/test` → `Test OK`, running on Temurin 21.
- **Why this and not the stock WildFly image.** `pom.xml` sets `maven.compiler.release` = `21`, so the WAR is Java 21 bytecode and needs a JDK 21 runtime. No WildFly or TomEE release that still ships the `javax.*` Java EE 8 APIs has a JDK 21 image (WildFly stops at `26.1.3.Final-jdk17`, TomEE at `8.0.16-jre17`; WildFly 27+ / TomEE 9+ moved to `jakarta.*`), and even the official Open Liberty images only go up to Java 17 — hence installing the Liberty runtime onto a Temurin 21 base by hand. The WebLogic path (`run.ps1`, WebLogic 14.1.2 on JDK 21) also satisfies both constraints.
- Any JDK 21+ can build it. To deploy on an older Java EE 8 server (WildFly 26, TomEE 8, GlassFish 5, Payara 5) instead, lower `maven.compiler.release` to `17` or `11` and point the `Dockerfile` back at that server's image.

## Endpoints

- `GET /sample-jee8/` → static `index.html`.
- `GET /sample-jee8/api/test` → `TestResource`, returns `"Test OK"` as JSON.

Path composition: context root `/sample-jee8` (from the per-server WEB-INF descriptors + `server.xml`) + `@ApplicationPath("api")` on `ApplicationConfig` + `@Path("test")` on the resource.

## Architecture notes

- **Artifact name vs. context root.** The WAR is named `SampleJEE8-*` (from `pom.xml` `artifactId`), but the deployed context root is `/sample-jee8`, set explicitly in `WEB-INF/jboss-web.xml`, `glassfish-web.xml`, `weblogic.xml`, and `ibm-web-ext.xml` (the Liberty one; without it, dropins deployment would use the WAR file name). If you change the context root, update all four (plus `README.md` URLs).
- **JAX-RS is activated with no `web.xml` servlet mapping.** `ApplicationConfig extends Application` with `@ApplicationPath` is the only registration; `web.xml` (Servlet 4.0) just declares the welcome file. `failOnMissingWebXml` is set to `false` in the POM so the WAR still builds if `web.xml` is removed.
- **CDI 2.0 is on** via `WEB-INF/beans.xml` (`version="2.0"`, `bean-discovery-mode="annotated"`). `TestResource` is `@RequestScoped`, so it's a CDI bean discovered by annotation scanning, not a registered JAX-RS singleton/class.
- **Multi-server support** is the reason for the parallel `ibm-web-ext.xml` / `jboss-web.xml` / `glassfish-web.xml` / `weblogic.xml` descriptors. `TestResource.java` comments record the per-server URLs (`:8080`, WebLogic `:7001`).

## Naming note

`jboss-web.xml` uses the schema-based form (`jboss-web_14_0.xsd`). `glassfish-web.xml` and `weblogic.xml` reference older DTD / schema public IDs; those servers are lenient about it and they are not in the Docker build path, so they are left as-is.
