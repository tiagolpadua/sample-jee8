package org.timsoft.api.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;

class BookMapperTest {

  private final BookMapper mapper = new BookMapper();

  private static BookRequest sampleRequest() {
    BookRequest r = new BookRequest();
    r.setTitle("Clean Code");
    r.setAuthor("Robert C. Martin");
    r.setIsbn("9780132350884");
    r.setPublishedYear(2008);
    r.setGenre(BookGenre.TECHNOLOGY);
    r.setPages(464);
    return r;
  }

  @Test
  void toEntityCopiesEveryField() {
    Book book = mapper.toEntity(sampleRequest());

    assertEquals("Clean Code", book.getTitle());
    assertEquals("Robert C. Martin", book.getAuthor());
    assertEquals("9780132350884", book.getIsbn());
    assertEquals(2008, book.getPublishedYear());
    assertEquals(BookGenre.TECHNOLOGY, book.getGenre());
    assertEquals(464, book.getPages());
  }

  @Test
  void applyPatchOnlyTouchesNonNullFields() {
    Book book = mapper.toEntity(sampleRequest());

    BookPatchRequest patch = new BookPatchRequest();
    patch.setTitle("Clean Code (2nd ed.)");
    patch.setPages(500);

    mapper.applyPatch(book, patch);

    assertEquals("Clean Code (2nd ed.)", book.getTitle());
    assertEquals(500, book.getPages());
    // untouched
    assertEquals("Robert C. Martin", book.getAuthor());
    assertEquals("9780132350884", book.getIsbn());
    assertEquals(BookGenre.TECHNOLOGY, book.getGenre());
  }

  @Test
  void toResponseMapsIdentityAndTimestamps() {
    Book book = mapper.toEntity(sampleRequest());
    book.setId(42L);
    book.onCreate();

    BookResponse response = mapper.toResponse(book);

    assertEquals(42L, response.getId());
    assertEquals("Clean Code", response.getTitle());
    assertEquals(book.getCreatedAt(), response.getCreatedAt());
    assertEquals(book.getUpdatedAt(), response.getUpdatedAt());
    assertNull(new BookResponse().getId());
  }
}
