package org.timsoft.api.openapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Serves the OpenAPI document for this application at {@code /api/openapi.json} and {@code
 * /api/openapi.yaml}.
 *
 * <p>Unlike Swagger's stock {@code io.swagger.v3.jaxrs2.integration.resources.OpenApiResource},
 * this does <b>not</b> scan the classpath with ClassGraph. On a full application server such as
 * WebLogic that scan walks every module on the server classpath and exhausts the heap ({@code
 * OutOfMemoryError} -> server panic). Here the annotation {@link Reader} runs over exactly the
 * classes registered in {@link Application}.
 */
@Path("/openapi.{type:json|yaml}")
public class OpenApiResource {

  private static final String YAML = "application/yaml";

  private static volatile OpenAPI cached;

  @Context private Application application;

  @GET
  @Produces({MediaType.APPLICATION_JSON, YAML})
  public Response getOpenApi(@PathParam("type") String type) {
    OpenAPI openApi = document();
    if ("yaml".equalsIgnoreCase(type)) {
      return Response.ok(Yaml.pretty(openApi)).type(YAML).build();
    }
    return Response.ok(Json.pretty(openApi)).type(MediaType.APPLICATION_JSON).build();
  }

  private OpenAPI document() {
    OpenAPI result = cached;
    if (result == null) {
      synchronized (OpenApiResource.class) {
        result = cached;
        if (result == null) {
          result = build();
          cached = result;
        }
      }
    }
    return result;
  }

  private OpenAPI build() {
    Set<Class<?>> classes = new HashSet<>();
    if (application != null) {
      if (application.getClasses() != null) {
        classes.addAll(application.getClasses());
      }
      if (application.getSingletons() != null) {
        application.getSingletons().forEach(s -> classes.add(s.getClass()));
      }
    }
    // Don't feed the doc generator to itself, and drop Swagger's own resources.
    classes.removeIf(c -> c.getName().startsWith("io.swagger."));
    classes.remove(OpenApiResource.class);

    OpenAPI base =
        new OpenAPI().info(new Info().title("sample-jee8 API").version("0.0.1-SNAPSHOT"));
    return new Reader(new SwaggerConfiguration().openAPI(base)).read(classes);
  }
}
