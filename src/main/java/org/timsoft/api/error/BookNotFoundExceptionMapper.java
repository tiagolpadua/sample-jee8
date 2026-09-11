package org.timsoft.api.error;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BookNotFoundExceptionMapper implements ExceptionMapper<BookNotFoundException> {

  @Context private UriInfo uriInfo;

  @Override
  public Response toResponse(BookNotFoundException ex) {
    ApiError entity = ApiError.of(Response.Status.NOT_FOUND, ex.getMessage(), path(), null);
    return Response.status(Response.Status.NOT_FOUND)
        .entity(entity)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private String path() {
    return uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
  }
}
