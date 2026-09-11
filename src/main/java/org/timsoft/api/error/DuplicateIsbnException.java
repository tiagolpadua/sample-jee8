package org.timsoft.api.error;

public class DuplicateIsbnException extends RuntimeException {

  public DuplicateIsbnException(String isbn) {
    super(String.format("A book with ISBN %s already exists", isbn));
  }
}
