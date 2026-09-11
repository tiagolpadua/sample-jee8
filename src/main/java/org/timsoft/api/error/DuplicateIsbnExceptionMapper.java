package org.timsoft.api.error;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DuplicateIsbnExceptionMapper implements ExceptionMapper<DuplicateIsbnException> {

  @Context private UriInfo uriInfo;

  @Override
  public Response toResponse(DuplicateIsbnException ex) {
    ApiError entity = ApiError.of(Response.Status.CONFLICT, ex.getMessage(), path(), null);
    return Response.status(Response.Status.CONFLICT)
        .entity(entity)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private String path() {
    return uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
  }
}
