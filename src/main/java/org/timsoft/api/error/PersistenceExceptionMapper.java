package org.timsoft.api.error;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

  @Context private UriInfo uriInfo;

  @Override
  public Response toResponse(PersistenceException ex) {
    log.warn("Persistence error while handling request", ex);
    ApiError entity =
        ApiError.of(Response.Status.CONFLICT, "Data integrity violation", path(), null);
    return Response.status(Response.Status.CONFLICT)
        .entity(entity)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private String path() {
    return uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
  }
}
