package org.timsoft.api.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

class ApiExceptionMappersTest {

  private static void setUriInfo(Object mapper, UriInfo uriInfo) {
    try {
      var f = mapper.getClass().getDeclaredField("uriInfo");
      f.setAccessible(true);
      f.set(mapper, uriInfo);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void bookNotFoundMapsTo404WithBody() {
    var mapper = new BookNotFoundExceptionMapper();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn("api/books/9");
    setUriInfo(mapper, uriInfo);

    Response response = mapper.toResponse(new BookNotFoundException(9L));

    assertEquals(404, response.getStatus());
    ApiError body = (ApiError) response.getEntity();
    assertEquals(404, body.getStatus());
    assertEquals("Not Found", body.getError());
    assertEquals("/api/books/9", body.getPath());
    assertEquals("Book 9 not found", body.getMessage());
  }

  @Test
  void duplicateIsbnMapsTo409() {
    var mapper = new DuplicateIsbnExceptionMapper();
    setUriInfo(mapper, null); // exercises the null-guard in path()

    Response response = mapper.toResponse(new DuplicateIsbnException("123"));

    assertEquals(409, response.getStatus());
    assertEquals(409, ((ApiError) response.getEntity()).getStatus());
  }

  @Test
  void constraintViolationMapsTo400WithPerFieldDetails() {
    var mapper = new ConstraintViolationExceptionMapper();

    Path.Node node = mock(Path.Node.class);
    when(node.getName()).thenReturn("title");
    Path path = mock(Path.class);
    Iterator<Path.Node> it = List.of(node).iterator();
    when(path.iterator()).thenReturn(it);
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");

    Response response = mapper.toResponse(new ConstraintViolationException(Set.of(violation)));

    assertEquals(400, response.getStatus());
    ApiError body = (ApiError) response.getEntity();
    assertEquals("Validation failed", body.getMessage());
    assertEquals(List.of("title: must not be blank"), body.getDetails());
  }

  @Test
  void persistenceExceptionMapsTo409() {
    var mapper = new PersistenceExceptionMapper();
    Response response = mapper.toResponse(new PersistenceException("unique index or primary key"));

    assertEquals(409, response.getStatus());
    assertEquals("Data integrity violation", ((ApiError) response.getEntity()).getMessage());
  }

  @Test
  void fallbackWrapsUnknownErrorAs500() {
    var mapper = new FallbackExceptionMapper();
    Response response = mapper.toResponse(new IllegalStateException("boom"));

    assertEquals(500, response.getStatus());
    assertEquals("Internal server error", ((ApiError) response.getEntity()).getMessage());
  }

  @Test
  void fallbackPassesThroughWebApplicationException() {
    var mapper = new FallbackExceptionMapper();
    NotFoundException wae = new NotFoundException();

    Response response = mapper.toResponse(wae);

    assertSame(wae.getResponse(), response);
    assertEquals(404, response.getStatus());
  }

  @Test
  void apiErrorFactoryDefaultsDetailsToEmptyList() {
    ApiError error = ApiError.of(Response.Status.BAD_REQUEST, "bad", "/x");

    assertEquals(400, error.getStatus());
    assertEquals("Bad Request", error.getError());
    assertTrue(error.getDetails().isEmpty());
    assertTrue(error.getTimestamp() != null);
  }
}
