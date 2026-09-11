package org.timsoft.api.error;

public class BookNotFoundException extends RuntimeException {

  public BookNotFoundException(Long id) {
    super(String.format("Book %d not found", id));
  }

  public BookNotFoundException(String message) {
    super(message);
  }
}
