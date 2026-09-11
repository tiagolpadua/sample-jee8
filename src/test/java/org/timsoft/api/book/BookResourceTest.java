package org.timsoft.api.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;
import org.timsoft.api.book.dto.PageResponse;

class BookResourceTest {

  private BookService service;
  private BookResource resource;

  @BeforeEach
  void setUp() {
    service = mock(BookService.class);
    resource = new BookResource();
    resource.service = service;
    resource.uriInfo = mock(UriInfo.class);
    when(resource.uriInfo.getAbsolutePathBuilder())
        .thenReturn(UriBuilder.fromUri("http://localhost:7001/sample-jee8/api/books"));
  }

  private static BookRequest request() {
    BookRequest r = new BookRequest();
    r.setTitle("Clean Code");
    r.setAuthor("Robert C. Martin");
    r.setIsbn("9780132350884");
    return r;
  }

  @Test
  void createReturns201WithLocationAndBody() {
    BookResponse dto = BookResponse.builder().id(7L).title("Clean Code").build();
    when(service.create(any(BookRequest.class))).thenReturn(dto);

    Response response = resource.create(request());

    assertEquals(201, response.getStatus());
    assertTrue(response.getLocation().toString().endsWith("/api/books/7"));
    assertSame(dto, response.getEntity());
  }

  @Test
  void listDelegatesToService() {
    PageResponse<BookResponse> page = PageResponse.of(List.of(), 0, 20, 0);
    when(service.list(null, null, null, 0, 20, "id,asc")).thenReturn(page);

    assertSame(page, resource.list(null, null, null, 0, 20, "id,asc"));
  }

  @Test
  void getDelegatesToService() {
    BookResponse dto = BookResponse.builder().id(1L).build();
    when(service.get(1L)).thenReturn(dto);

    assertSame(dto, resource.get(1L));
  }

  @Test
  void updateDelegatesToService() {
    BookResponse dto = BookResponse.builder().id(1L).build();
    when(service.update(eq(1L), any(BookRequest.class))).thenReturn(dto);

    assertSame(dto, resource.update(1L, request()));
  }

  @Test
  void patchDelegatesToService() {
    BookResponse dto = BookResponse.builder().id(1L).build();
    BookPatchRequest patch = new BookPatchRequest();
    when(service.patch(eq(1L), any(BookPatchRequest.class))).thenReturn(dto);

    assertSame(dto, resource.patch(1L, patch));
  }

  @Test
  void deleteReturns204AndCallsService() {
    Response response = resource.delete(3L);

    assertEquals(204, response.getStatus());
    verify(service).delete(3L);
  }

  private static <T> T any(Class<T> type) {
    return org.mockito.ArgumentMatchers.any(type);
  }
}
