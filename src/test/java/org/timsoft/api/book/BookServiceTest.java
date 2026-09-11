package org.timsoft.api.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.timsoft.api.book.dto.BookPatchRequest;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;
import org.timsoft.api.error.BookNotFoundException;
import org.timsoft.api.error.DuplicateIsbnException;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock BookRepository repository;
  @Mock BookMapper mapper;
  @InjectMocks BookService service;

  private BookRequest request;

  @BeforeEach
  void setUp() {
    request = new BookRequest();
    request.setTitle("Clean Code");
    request.setAuthor("Robert C. Martin");
    request.setIsbn("9780132350884");
    request.setPublishedYear(2008);
    request.setGenre(BookGenre.TECHNOLOGY);
    request.setPages(464);
  }

  @Test
  void createPersistsWhenIsbnIsFree() {
    Book entity = new Book();
    Book saved = new Book();
    saved.setId(1L);
    when(repository.existsByIsbn("9780132350884")).thenReturn(false);
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(BookResponse.builder().id(1L).build());

    BookResponse result = service.create(request);

    assertEquals(1L, result.getId());
    verify(repository).save(entity);
  }

  @Test
  void createRejectsDuplicateIsbn() {
    when(repository.existsByIsbn("9780132350884")).thenReturn(true);

    assertThrows(DuplicateIsbnException.class, () -> service.create(request));
    verify(repository, never()).save(any());
  }

  @Test
  void createRejectsPublishedYearTooFarInTheFuture() {
    request.setPublishedYear(Year.now().getValue() + 5);

    assertThrows(RuntimeException.class, () -> service.create(request));
    verify(repository, never()).save(any());
  }

  @Test
  void getThrowsWhenMissing() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BookNotFoundException.class, () -> service.get(99L));
  }

  @Test
  void updateThrowsWhenMissing() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BookNotFoundException.class, () -> service.update(99L, request));
  }

  @Test
  void updateRejectsIsbnAlreadyUsedByAnotherBook() {
    Book existing = new Book();
    existing.setId(1L);
    existing.setIsbn("0000000000000");
    when(repository.findById(1L)).thenReturn(Optional.of(existing));
    when(repository.existsByIsbn("9780132350884")).thenReturn(true);

    assertThrows(DuplicateIsbnException.class, () -> service.update(1L, request));
  }

  @Test
  void patchKeepingSameIsbnDoesNotConflict() {
    Book existing = new Book();
    existing.setId(1L);
    existing.setIsbn("9780132350884");
    when(repository.findById(1L)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    when(mapper.toResponse(existing)).thenReturn(BookResponse.builder().id(1L).build());

    BookPatchRequest patch = new BookPatchRequest();
    patch.setIsbn("9780132350884");
    patch.setPages(500);

    service.patch(1L, patch);

    verify(mapper).applyPatch(existing, patch);
  }

  @Test
  void deleteThrowsWhenNothingRemoved() {
    when(repository.deleteById(99L)).thenReturn(false);

    assertThrows(BookNotFoundException.class, () -> service.delete(99L));
  }

  @Test
  void listClampsPageSizeAndDelegates() {
    when(repository.count(any())).thenReturn(0L);
    when(repository.search(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
        .thenReturn(List.of());

    var page = service.list(null, null, null, -3, 9999, null);

    assertEquals(0, page.getPage());
    assertEquals(BookService.MAX_PAGE_SIZE, page.getSize());
  }
}
