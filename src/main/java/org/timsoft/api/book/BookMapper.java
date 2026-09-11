package org.timsoft.api.book;

import javax.enterprise.context.ApplicationScoped;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;

/** Converts between {@link Book} entities and the DTOs. */
@ApplicationScoped
public class BookMapper {

  public Book toEntity(BookRequest request) {
    Book book = new Book();
    apply(book, request);
    return book;
  }

  /** Full replacement (PUT): every field of {@code request} is copied over. */
  public void apply(Book book, BookRequest request) {
    book.setTitle(request.getTitle());
    book.setAuthor(request.getAuthor());
    book.setIsbn(request.getIsbn());
    book.setPublishedYear(request.getPublishedYear());
    book.setGenre(request.getGenre());
    book.setPages(request.getPages());
  }

  /** Partial update (PATCH): only non-null fields of {@code patch} are copied over. */
  public void applyPatch(Book book, BookPatchRequest patch) {
    if (patch.getTitle() != null) {
      book.setTitle(patch.getTitle());
    }
    if (patch.getAuthor() != null) {
      book.setAuthor(patch.getAuthor());
    }
    if (patch.getIsbn() != null) {
      book.setIsbn(patch.getIsbn());
    }
    if (patch.getPublishedYear() != null) {
      book.setPublishedYear(patch.getPublishedYear());
    }
    if (patch.getGenre() != null) {
      book.setGenre(patch.getGenre());
    }
    if (patch.getPages() != null) {
      book.setPages(patch.getPages());
    }
  }

  public BookResponse toResponse(Book book) {
    return BookResponse.builder()
        .id(book.getId())
        .title(book.getTitle())
        .author(book.getAuthor())
        .isbn(book.getIsbn())
        .publishedYear(book.getPublishedYear())
        .genre(book.getGenre())
        .pages(book.getPages())
        .createdAt(book.getCreatedAt())
        .updatedAt(book.getUpdatedAt())
        .build();
  }
}
