package org.timsoft.api.book;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;
import org.timsoft.api.book.dto.PageResponse;

@Path("books")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "books")
public class BookResource {

  @Inject BookService service;

  @Context UriInfo uriInfo;

  @POST
  @Operation(summary = "Create a book")
  public Response create(@Valid BookRequest request) {
    BookResponse created = service.create(request);
    URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getId())).build();
    return Response.created(location).entity(created).build();
  }

  @GET
  @Operation(summary = "List books (paged, filterable by title/author/genre)")
  public PageResponse<BookResponse> list(
      @QueryParam("title") String title,
      @QueryParam("author") String author,
      @QueryParam("genre") BookGenre genre,
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("20") int size,
      @QueryParam("sort") @DefaultValue("id,asc") String sort) {
    return service.list(title, author, genre, page, size, sort);
  }

  @GET
  @Path("{id}")
  @Operation(summary = "Get a book by id")
  public BookResponse get(@PathParam("id") Long id) {
    return service.get(id);
  }

  @PUT
  @Path("{id}")
  @Operation(summary = "Replace a book")
  public BookResponse update(@PathParam("id") Long id, @Valid BookRequest request) {
    return service.update(id, request);
  }

  @PATCH
  @Path("{id}")
  @Operation(summary = "Partially update a book")
  public BookResponse patch(@PathParam("id") Long id, @Valid BookPatchRequest patch) {
    return service.patch(id, patch);
  }

  @DELETE
  @Path("{id}")
  @Operation(summary = "Delete a book")
  public Response delete(@PathParam("id") Long id) {
    service.delete(id);
    return Response.noContent().build();
  }
}
