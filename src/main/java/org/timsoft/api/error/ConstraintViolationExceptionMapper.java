package org.timsoft.api.error;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Priority;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER - 100)
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  @Context private UriInfo uriInfo;

  @Override
  public Response toResponse(ConstraintViolationException ex) {
    List<String> details = new ArrayList<>();
    for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
      details.add(lastPathNode(v.getPropertyPath()) + ": " + v.getMessage());
    }
    ApiError entity =
        ApiError.of(Response.Status.BAD_REQUEST, "Validation failed", path(), details);
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(entity)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private static String lastPathNode(Path propertyPath) {
    String name = null;
    for (Path.Node node : propertyPath) {
      if (node.getName() != null) {
        name = node.getName();
      }
    }
    return name != null ? name : propertyPath.toString();
  }

  private String path() {
    return uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
  }
}
