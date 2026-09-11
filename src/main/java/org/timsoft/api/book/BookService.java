package org.timsoft.api.book;

import java.time.Year;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;
import org.timsoft.api.book.dto.PageResponse;
import org.timsoft.api.error.BookNotFoundException;
import org.timsoft.api.error.DuplicateIsbnException;
import org.timsoft.api.persistence.Tx;

/** Business rules for the Book CRUD. Write operations run inside a {@link Tx} transaction. */
@ApplicationScoped
public class BookService {

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

  @Inject BookRepository repository;
  @Inject BookMapper mapper;

  @Tx
  public BookResponse create(BookRequest request) {
    validateYear(request.getPublishedYear());
    if (repository.existsByIsbn(request.getIsbn())) {
      throw new DuplicateIsbnException(request.getIsbn());
    }
    Book saved = repository.save(mapper.toEntity(request));
    return mapper.toResponse(saved);
  }

  public BookResponse get(Long id) {
    return repository
        .findById(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new BookNotFoundException(id));
  }

  public PageResponse<BookResponse> list(
      String title, String author, BookGenre genre, int page, int size, String sort) {
    int safePage = Math.max(page, 0);
    int safeSize = clampSize(size);
    Sort parsedSort = Sort.parse(sort);

    BookFilter filter = BookFilter.of(title, author, genre);
    long total = repository.count(filter);
    List<BookResponse> items =
        repository
            .search(filter, safePage, safeSize, parsedSort.field(), parsedSort.ascending())
            .stream()
            .map(mapper::toResponse)
            .toList();

    return PageResponse.of(items, safePage, safeSize, total);
  }

  @Tx
  public BookResponse update(Long id, BookRequest request) {
    validateYear(request.getPublishedYear());
    Book book = repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    ensureIsbnFree(request.getIsbn(), book);
    mapper.apply(book, request);
    return mapper.toResponse(repository.save(book));
  }

  @Tx
  public BookResponse patch(Long id, BookPatchRequest patch) {
    if (patch.getPublishedYear() != null) {
      validateYear(patch.getPublishedYear());
    }
    Book book = repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    if (patch.getIsbn() != null) {
      ensureIsbnFree(patch.getIsbn(), book);
    }
    mapper.applyPatch(book, patch);
    return mapper.toResponse(repository.save(book));
  }

  @Tx
  public void delete(Long id) {
    if (!repository.deleteById(id)) {
      throw new BookNotFoundException(id);
    }
  }

  private void ensureIsbnFree(String isbn, Book current) {
    if (isbn.equals(current.getIsbn())) {
      return;
    }
    if (repository.existsByIsbn(isbn)) {
      throw new DuplicateIsbnException(isbn);
    }
  }

  private void validateYear(Integer year) {
    if (year != null && year > Year.now().getValue() + 1) {
      throw new ValidationException("publishedYear: must not be after next year");
    }
  }

  private int clampSize(int size) {
    if (size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }

  /**
   * {@code sort=field,asc|desc}; defaults to {@code id,asc}. Only a small allow-list is accepted.
   */
  record Sort(String field, boolean ascending) {
    private static final List<String> ALLOWED =
        List.of(
            "id", "title", "author", "publishedYear", "genre", "pages", "createdAt", "updatedAt");

    static Sort parse(String raw) {
      if (raw == null || raw.isBlank()) {
        return new Sort("id", true);
      }
      String[] parts = raw.split(",", 2);
      String field = parts[0].trim();
      if (!ALLOWED.contains(field)) {
        throw new ValidationException("sort: unknown field '" + field + "'");
      }
      boolean ascending = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());
      return new Sort(field, ascending);
    }
  }
}
