package org.timsoft.api;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.timsoft.api.test.TestResource;

@OpenAPIDefinition(info = @Info(title = "sample-jee8 API", version = "0.0.1-SNAPSHOT"))
@ApplicationPath("api")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(
        TestResource.class,
        // serves GET /api/openapi.json and /api/openapi.yaml
        OpenApiResource.class);
  }
}
