package org.timsoft.api.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import lombok.Data;
import org.timsoft.api.book.BookGenre;

@Data
public class BookRequest {

  @NotBlank @Size(max = 200) @Schema(example = "The Pragmatic Programmer")
  private String title;

  @NotBlank @Size(max = 120) @Schema(example = "Andrew Hunt")
  private String author;

  @NotBlank @Size(min = 10, max = 20) @Pattern(regexp = "[0-9Xx][0-9Xx -]{8,18}[0-9Xx]", message = "must be a valid ISBN-10/13") @Schema(example = "978-0135957059")
  private String isbn;

  @Min(1450) private Integer publishedYear;

  private BookGenre genre;

  @Positive private Integer pages;
}
