# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Java EE 8 (`javax:javaee-api` 8.0.1, `provided` scope) skeleton web app packaged as a WAR. It exposes a single JAX-RS resource and exists as a starting point / deployment sanity check for **Oracle WebLogic 14.1.2**. There is no persistence, no business logic, and no test code (`src/test` holds only `.gitkeep`).

The code uses the pre-Jakarta `javax.*` namespace throughout (`javax.ws.rs.*`, `javax.enterprise.context.*`) and the Java EE 8 XML descriptor namespace `http://xmlns.jcp.org/xml/ns/javaee`. (Jakarta EE 9 later renamed every `javax.*` EE package to `jakarta.*`; this project predates that.) The API surface is Java EE 8, but the code is **compiled for JDK 21** (`maven.compiler.release` = `21`) — WebLogic 14.1.2 runs on JDK 21, so this is fine.

## Build & run

- Build the WAR: `./mvnw.cmd clean install` (or `mvn-clean-install.ps1`) → produces `target/SampleJEE8-0.0.1-SNAPSHOT.war`. Requires JDK 21+.
- Deploy to WebLogic: `autodeploy.ps1` — sets `JAVA_HOME` to the WebLogic-bundled JDK 21 (`C:\KDI\wl_14.1.2.0.0\jdk-21.0.12.1`), builds, then refreshes the WAR in the `base_domain` autodeploy folder:
  `C:\KDI\wl_14.1.2.0.0\Oracle\Middleware\Oracle_Home\user_projects\domains\base_domain\autodeploy`.
  WebLogic in development mode picks it up automatically.
- App is served at `http://localhost:7001/sample-jee8` (default WebLogic port).

## Endpoints

- `GET /sample-jee8/` → static `index.html`.
- `GET /sample-jee8/api/test` → `TestResource`, returns `"Test OK"` as JSON.

Path composition: context root `/sample-jee8` (from `WEB-INF/weblogic.xml`) + `@ApplicationPath("api")` on `ApplicationConfig` + `@Path("test")` on the resource.

## Architecture notes

- **Artifact name vs. context root.** The WAR is named `SampleJEE8-*` (from `pom.xml` `artifactId`), but the deployed context root is `/sample-jee8`, set in `WEB-INF/weblogic.xml`. If you change the context root, update `weblogic.xml` and `README.md`.
- **JAX-RS is activated with no `web.xml` servlet mapping.** `ApplicationConfig extends Application` with `@ApplicationPath` is the only registration; `web.xml` (Servlet 4.0) just declares the welcome file. `failOnMissingWebXml` is set to `false` in the POM so the WAR still builds if `web.xml` is removed.
- **CDI 2.0 is on** via `WEB-INF/beans.xml` (`version="2.0"`, `bean-discovery-mode="annotated"`). `TestResource` is `@RequestScoped`, so it's a CDI bean discovered by annotation scanning, not a registered JAX-RS singleton/class.
