package org.timsoft.api.book;

/** Optional list/search filters for {@link Book}. Any field may be {@code null} (ignored). */
public record BookFilter(String title, String author, BookGenre genre) {

  public static BookFilter of(String title, String author, BookGenre genre) {
    return new BookFilter(title, author, genre);
  }
}
