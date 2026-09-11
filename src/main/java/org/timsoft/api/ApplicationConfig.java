package org.timsoft.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import java.util.Map;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Per the JAX-RS spec (2.3.2): when an {@code Application} subclass with {@code @ApplicationPath}
 * does NOT override {@code getClasses()}/{@code getSingletons()} (both default to empty), the
 * container MUST scan the deployment (WEB-INF/classes + WEB-INF/lib) for {@code @Path} /
 * {@code @Provider} classes and register them itself. That covers every resource and exception
 * mapper in this WAR, plus swagger-jaxrs2's own {@code OpenApiResource} and this app's own {@code
 * json.AppJacksonJsonProvider}.
 *
 * <p>This is unrelated to - and much narrower than - the classpath scan that crashed WebLogic (see
 * openapi-configuration.yaml): that one was Swagger's own ClassGraph scanner walking the whole
 * application-server classpath. This is the container's own deployment-scoped discovery.
 *
 * <p>If a resource or mapper isn't picked up on WebLogic, override {@code getClasses()} again and
 * list it explicitly - don't fight the container for a class it insists on missing.
 *
 * <p>{@code servers} pins the OpenAPI doc's base URL to the context root. Without it, tools that
 * build requests from the spec (Swagger UI's "Try it out", curl generators, ...) assume the app is
 * deployed at the server root and call {@code http://host:port/api/...} instead of {@code
 * http://host:port/sample-jee8/api/...}. Keep this in sync with the context root set in {@code
 * WEB-INF/weblogic.xml} (see the "Artifact name vs. context root" note in CLAUDE.md).
 */
@OpenAPIDefinition(
    info = @Info(title = "sample-jee8 API", version = "0.0.1-SNAPSHOT"),
    servers = @Server(url = "/sample-jee8"))
@ApplicationPath("api")
public class ApplicationConfig extends Application {

  /**
   * {@code getProperties()} is a separate JAX-RS hook from {@code getClasses()} - overriding it
   * doesn't disable the container-scoped auto-discovery documented above.
   *
   * <p>Explicitly disables the JSON-B and MOXy JAX-RS providers WebLogic's Jersey would otherwise
   * auto-discover, so {@code json.AppJacksonJsonProvider} (Jackson) is the only JSON provider left
   * - see the "JSON serialization" section of CLAUDE.md. Property names confirmed against the
   * actual jersey-media-json-binding / jersey-media-moxy classes WebLogic 14.1.2 bundles.
   */
  @Override
  public Map<String, Object> getProperties() {
    return Map.of(
        "jersey.config.disableJsonBinding", true,
        "jersey.config.disableMoxyJson", true);
  }
}
