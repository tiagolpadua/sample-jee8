package org.timsoft.api.book.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.timsoft.api.book.BookGenre;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

  private Long id;
  private String title;
  private String author;
  private String isbn;
  private Integer publishedYear;
  private BookGenre genre;
  private Integer pages;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
