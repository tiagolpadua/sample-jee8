package org.timsoft.api.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Exercises {@link BookRepository} against a real in-memory H2 database via EclipseLink. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookRepositoryTest {

  private EntityManagerFactory emf;
  private EntityManager em;
  private BookRepository repository;

  @BeforeAll
  void bootstrap() {
    emf = Persistence.createEntityManagerFactory("sampledbPU");
    // force EclipseLink to deploy the unit and run schema generation now
    emf.createEntityManager().close();
  }

  @AfterAll
  void teardown() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @BeforeEach
  void freshEntityManager() {
    if (em != null && em.isOpen()) {
      em.close();
    }
    em = emf.createEntityManager();
    repository = new BookRepository();
    inject(repository, em);
    inTx(
        e -> {
          e.createQuery("DELETE FROM Book").executeUpdate();
          return null;
        });
  }

  @Test
  void saveAssignsIdAndTimestamps() {
    Book book =
        inTx(
            e ->
                repository.save(
                    newBook("Clean Code", "Martin", "111", BookGenre.TECHNOLOGY, 2008)));

    assertTrue(book.getId() != null);
    assertTrue(book.getCreatedAt() != null);
    assertEquals(book.getCreatedAt(), book.getUpdatedAt());
    assertEquals(1L, book.getVersion()); // EclipseLink starts @Version at 1
  }

  @Test
  void findByIdReturnsPersistedRow() {
    Long id =
        inTx(e -> repository.save(newBook("Dune", "Herbert", "222", BookGenre.FICTION, 1965)))
            .getId();

    em.clear();
    assertTrue(repository.findById(id).isPresent());
    assertTrue(repository.findById(-1L).isEmpty());
  }

  @Test
  void existsAndFindByIsbn() {
    inTx(e -> repository.save(newBook("Sapiens", "Harari", "333", BookGenre.HISTORY, 2011)));

    assertTrue(repository.existsByIsbn("333"));
    assertFalse(repository.existsByIsbn("nope"));
    assertEquals("Sapiens", repository.findByIsbn("333").orElseThrow().getTitle());
  }

  @Test
  void deleteByIdReportsWhetherRowExisted() {
    Long id =
        inTx(e ->
                repository.save(
                    newBook("Refactoring", "Fowler", "444", BookGenre.TECHNOLOGY, 1999)))
            .getId();

    boolean firstDelete = inTx(e -> repository.deleteById(id));
    boolean secondDelete = inTx(e -> repository.deleteById(id));
    assertTrue(firstDelete);
    assertFalse(secondDelete);
  }

  @Test
  void searchFiltersPagesAndCounts() {
    inTx(
        e -> {
          repository.save(newBook("Java Concurrency", "Goetz", "a1", BookGenre.TECHNOLOGY, 2006));
          repository.save(newBook("Effective Java", "Bloch", "a2", BookGenre.TECHNOLOGY, 2018));
          repository.save(newBook("Deep Learning", "Goodfellow", "a3", BookGenre.SCIENCE, 2016));
          repository.save(
              newBook("Clean Architecture", "Martin", "a4", BookGenre.TECHNOLOGY, 2017));
          return null;
        });

    BookFilter techFilter = BookFilter.of(null, null, BookGenre.TECHNOLOGY);
    assertEquals(3, repository.count(techFilter));

    List<Book> firstPage = repository.search(techFilter, 0, 2, "title", true);
    assertEquals(2, firstPage.size());
    assertEquals("Clean Architecture", firstPage.get(0).getTitle());

    List<Book> byAuthor = repository.search(BookFilter.of(null, "martin", null), 0, 10, "id", true);
    assertEquals(1, byAuthor.size());
    assertEquals("Clean Architecture", byAuthor.get(0).getTitle());
  }

  @Test
  void versionIncrementsOnUpdate() {
    Long id =
        inTx(e ->
                repository.save(newBook("Grokking", "Bhargava", "v1", BookGenre.TECHNOLOGY, 2016)))
            .getId();

    inTx(
        e -> {
          Book managed = repository.findById(id).orElseThrow();
          managed.setPages(256);
          repository.save(managed);
          return null;
        });

    em.clear();
    assertEquals(
        2L, repository.findById(id).orElseThrow().getVersion()); // 1 on insert, +1 on update
  }

  // --- helpers ---------------------------------------------------------------

  private static Book newBook(String title, String author, String isbn, BookGenre genre, int year) {
    Book b = new Book();
    b.setTitle(title);
    b.setAuthor(author);
    b.setIsbn(isbn);
    b.setGenre(genre);
    b.setPublishedYear(year);
    b.setPages(300);
    return b;
  }

  private <T> T inTx(Function<EntityManager, T> work) {
    em.getTransaction().begin();
    try {
      T result = work.apply(em);
      em.getTransaction().commit();
      return result;
    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      throw ex;
    }
  }

  private static void inject(BookRepository repository, EntityManager em) {
    try {
      var field = BookRepository.class.getDeclaredField("em");
      field.setAccessible(true);
      field.set(repository, em);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
