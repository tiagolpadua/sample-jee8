package org.timsoft.api;

import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.timsoft.api.openapi.OpenApiResource;
import org.timsoft.api.test.TestResource;

@ApplicationPath("api")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(
        TestResource.class,
        // serves GET /api/openapi.json and /api/openapi.yaml (no classpath scan)
        OpenApiResource.class);
  }
}
