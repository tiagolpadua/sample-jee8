# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Java EE 8 (`javaee-api` 8.0, `provided` scope) skeleton web app packaged as a WAR. It exposes a single JAX-RS resource and exists as a starting point / deployment sanity check across multiple application servers (WildFly/JBoss, GlassFish, WebLogic). There is no persistence, no business logic, and no test code (`src/test` holds only `.gitkeep`).

Java EE 8 still uses the `javax.*` namespace — the `jakarta.*` rename only happened in Jakarta EE 9, so no source imports change relative to the old EE 7 setup.

## Build & run

- Build the WAR: `mvn clean install` → produces `target/SampleJEE8-0.0.1-SNAPSHOT.war`.
- The helper scripts (`mvn-clean-install.sh`, `autodeploy.sh`) hardcode a macOS `JAVA_HOME` for a JDK 8 install and a GlassFish `autodeploy` path — they are machine-specific to the original author and will not work as-is elsewhere. Treat them as examples, not entry points.
- Run in Docker (WildFly): `./run.sh` — runs `mvn clean install` then `docker-compose up --no-deps --build`. The `Dockerfile` (`jboss/wildfly:18.0.0.Final`, a Java EE 8 full-profile server) copies `target/Sample*.war` into WildFly's `deployments/`, so the Maven build must run first. App is served at `http://localhost:8080/sample-jee8`.
- Requires JDK 8 — `pom.xml` pins `maven.compiler.source`/`target` to `1.8` (Java SE 8 is the EE 8 baseline).

## Endpoints

- `GET /sample-jee8/` → static `index.html`.
- `GET /sample-jee8/api/test` → `TestResource`, returns `"Test OK"` as JSON.

Path composition: context root `/sample-jee8` (from the per-server WEB-INF descriptors) + `@ApplicationPath("api")` on `ApplicationConfig` + `@Path("test")` on the resource.

## Architecture notes

- **Artifact name vs. context root.** The WAR is named `SampleJEE8-*` (from `pom.xml` `artifactId`), but the deployed context root is `/sample-jee8`, set explicitly in `WEB-INF/jboss-web.xml`, `glassfish-web.xml`, and `weblogic.xml`. If you change the context root, update all three (plus `README.md` URLs).
- **JAX-RS is activated with no `web.xml` servlet mapping.** `ApplicationConfig extends Application` with `@ApplicationPath` is the only registration; `web.xml` (Servlet 4.0) just declares the welcome file. `failOnMissingWebXml` is set to `false` in the POM so the WAR still builds if `web.xml` is removed.
- **CDI 2.0 is on** via `WEB-INF/beans.xml` (`version="2.0"`, `bean-discovery-mode="annotated"`). `TestResource` is `@RequestScoped`, so it's a CDI bean discovered by annotation scanning, not a registered JAX-RS singleton/class.
- **Multi-server support** is the reason for the parallel `jboss-web.xml` / `glassfish-web.xml` / `weblogic.xml` descriptors. `TestResource.java` comments record the per-server URLs (WildFly `:8080`, WebLogic `:7001`).

## Naming note

The repo directory, git default branch (`main`), and all identifiers now say `jee8` / `SampleJEE8`. The `glassfish-web.xml` DTD still references the GlassFish 3.1 / Servlet 3.0 public ID — that DTD is unchanged through GlassFish 5 and is left as-is.
