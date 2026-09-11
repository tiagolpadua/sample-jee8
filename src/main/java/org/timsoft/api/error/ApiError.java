package org.timsoft.api.error;

import java.time.Instant;
import java.util.List;
import javax.ws.rs.core.Response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiError {

  private Instant timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
  private List<String> details;

  public static ApiError of(
      Response.Status status, String message, String path, List<String> details) {
    return ApiError.builder()
        .timestamp(Instant.now())
        .status(status.getStatusCode())
        .error(status.getReasonPhrase())
        .message(message)
        .path(path)
        .details(details == null ? List.of() : details)
        .build();
  }

  public static ApiError of(Response.Status status, String message, String path) {
    return of(status, message, path, null);
  }
}
