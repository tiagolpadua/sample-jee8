package org.timsoft.api.book.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import lombok.Data;
import org.timsoft.api.book.BookGenre;

@Data
public class BookPatchRequest {

  @Size(max = 200) private String title;

  @Size(max = 120) private String author;

  @Size(min = 10, max = 20) @Pattern(regexp = "[0-9Xx][0-9Xx -]{8,18}[0-9Xx]", message = "must be a valid ISBN-10/13") private String isbn;

  private Integer publishedYear;

  private BookGenre genre;

  @Positive private Integer pages;
}
