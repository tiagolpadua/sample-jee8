package org.timsoft.api;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.timsoft.api.book.BookResource;
import org.timsoft.api.error.BookNotFoundExceptionMapper;
import org.timsoft.api.error.ConstraintViolationExceptionMapper;
import org.timsoft.api.error.DuplicateIsbnExceptionMapper;
import org.timsoft.api.error.FallbackExceptionMapper;
import org.timsoft.api.error.PersistenceExceptionMapper;
import org.timsoft.api.test.TestResource;

@OpenAPIDefinition(info = @Info(title = "sample-jee8 API", version = "0.0.1-SNAPSHOT"))
@ApplicationPath("api")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(
        TestResource.class,
        BookResource.class,
        // serves GET /api/openapi.json and /api/openapi.yaml; scan is scoped by
        // resourcePackages in src/main/resources/openapi-configuration.yaml
        OpenApiResource.class,
        BookNotFoundExceptionMapper.class,
        DuplicateIsbnExceptionMapper.class,
        ConstraintViolationExceptionMapper.class,
        PersistenceExceptionMapper.class,
        FallbackExceptionMapper.class);
  }
}
