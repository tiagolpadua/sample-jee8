package org.timsoft.api.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Data access for {@link Book}. Uses the request-scoped, application-managed {@link EntityManager}.
 */
@ApplicationScoped
public class BookRepository {

  @Inject EntityManager em;

  public Book save(Book book) {
    if (book.getId() == null) {
      em.persist(book);
      return book;
    }
    return em.merge(book);
  }

  public Optional<Book> findById(Long id) {
    return Optional.ofNullable(em.find(Book.class, id));
  }

  public Optional<Book> findByIsbn(String isbn) {
    return em
        .createQuery("SELECT b FROM Book b WHERE b.isbn = :isbn", Book.class)
        .setParameter("isbn", isbn)
        .getResultList()
        .stream()
        .findFirst();
  }

  public boolean existsByIsbn(String isbn) {
    Long count =
        em.createQuery("SELECT COUNT(b) FROM Book b WHERE b.isbn = :isbn", Long.class)
            .setParameter("isbn", isbn)
            .getSingleResult();
    return count != null && count > 0;
  }

  public boolean deleteById(Long id) {
    Book managed = em.find(Book.class, id);
    if (managed == null) {
      return false;
    }
    em.remove(managed);
    return true;
  }

  public long count(BookFilter filter) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<Book> root = query.from(Book.class);
    query.select(cb.count(root)).where(toPredicates(cb, root, filter));
    return em.createQuery(query).getSingleResult();
  }

  public List<Book> search(
      BookFilter filter, int page, int size, String sortField, boolean ascending) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Book> query = cb.createQuery(Book.class);
    Root<Book> root = query.from(Book.class);
    query.select(root).where(toPredicates(cb, root, filter));

    String field = sortField == null || sortField.isBlank() ? "id" : sortField;
    query.orderBy(ascending ? cb.asc(root.get(field)) : cb.desc(root.get(field)));

    return em.createQuery(query)
        .setFirstResult(Math.max(page, 0) * size)
        .setMaxResults(size)
        .getResultList();
  }

  private Predicate[] toPredicates(CriteriaBuilder cb, Root<Book> root, BookFilter filter) {
    List<Predicate> predicates = new ArrayList<>();
    if (filter != null) {
      if (filter.title() != null && !filter.title().isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("title")), like(filter.title())));
      }
      if (filter.author() != null && !filter.author().isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("author")), like(filter.author())));
      }
      if (filter.genre() != null) {
        predicates.add(cb.equal(root.get("genre"), filter.genre()));
      }
    }
    return predicates.toArray(new Predicate[0]);
  }

  private static String like(String value) {
    return "%" + value.toLowerCase() + "%";
  }
}
