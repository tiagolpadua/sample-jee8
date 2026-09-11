package org.timsoft.api.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * The {@link ObjectMapper} used for every {@code application/json} request/response body. {@link
 * AppJacksonJsonProvider} looks this up via {@code Providers.getContextResolver} before falling
 * back to a bare {@code new ObjectMapper()}, which has no {@code java.time.LocalDateTime} support.
 *
 * <p>{@code WRITE_DATES_AS_TIMESTAMPS} is off: {@code JavaTimeModule} alone still serializes {@code
 * LocalDateTime} as a numeric array ({@code [2026,9,10,21,0]}), not ISO-8601 - verified by actually
 * calling this provider end to end, not assumed. ISO-8601 is what the OpenAPI schema declares
 * ({@code format: date-time}) and what the API returned before this class existed.
 */
@Provider
public class AppObjectMapperProvider implements ContextResolver<ObjectMapper> {

  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }
}
