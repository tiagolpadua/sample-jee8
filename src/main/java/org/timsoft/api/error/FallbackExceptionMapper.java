package org.timsoft.api.error;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@Priority(Priorities.USER + 1000)
public class FallbackExceptionMapper implements ExceptionMapper<Throwable> {

  @Context private UriInfo uriInfo;

  @Override
  public Response toResponse(Throwable ex) {
    if (ex instanceof WebApplicationException wae) {
      return wae.getResponse();
    }
    log.error("Unhandled exception while handling request", ex);
    ApiError entity =
        ApiError.of(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error", path(), null);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(entity)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private String path() {
    return uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
  }
}
