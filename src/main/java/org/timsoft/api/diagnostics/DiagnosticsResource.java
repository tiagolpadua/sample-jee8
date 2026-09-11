package org.timsoft.api.diagnostics;

import io.swagger.v3.oas.annotations.Hidden;
import java.lang.annotation.Annotation;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import org.timsoft.api.book.dto.BookRequest;
import org.timsoft.api.book.dto.BookResponse;

/**
 * Ops/diagnostic endpoint: asks the actual JAX-RS runtime which {@code MessageBodyWriter}/{@code
 * MessageBodyReader} it uses for JSON on this deployment (Jackson vs MOXy vs JSON-B vs whatever
 * else WebLogic's bundled Jersey has on its classpath). What's installed on disk doesn't tell you
 * what gets selected - only the runtime's own provider lookup does; this is that lookup, exposed.
 *
 * <p>WebLogic 14.1.2's Jersey picks JSON-B (Yasson 1.0.3) here by default - see below for what this
 * endpoint reported before {@code json.AppJacksonJsonProvider} + {@code
 * ApplicationConfig.getProperties()} forced Jackson on instead (see the "JSON serialization"
 * section of CLAUDE.md). Expect {@code org.timsoft.api.json.AppJacksonJsonProvider} now; if it ever
 * reports JSON-B or MOXy again, that fix regressed - check {@code
 * ApplicationConfig.getProperties()} and that the provider classes are still being discovered.
 * Worth re-checking after a WebLogic upgrade or domain change either way.
 *
 * <p>{@code GET /sample-jee8/api/debug/json-provider}. Kept out of the public API surface
 * ({@code @Hidden} excludes it from the OpenAPI doc) and out of the JaCoCo coverage gate in
 * pom.xml, since it only makes sense exercised against a real container.
 */

// Default WL14, before AppJacksonJsonProvider / getProperties() forced Jackson on:
// writer (BookResponse -> JSON): org.glassfish.jersey.jsonb.internal.JsonBindingProvider
// reader (JSON -> BookRequest): org.glassfish.jersey.jsonb.internal.JsonBindingProvider

@Hidden
@Path("debug")
@RequestScoped
public class DiagnosticsResource {

  @GET
  @Path("json-provider")
  @Produces(MediaType.TEXT_PLAIN)
  public String jsonProvider(@Context Providers providers) {
    MessageBodyWriter<BookResponse> writer =
        providers.getMessageBodyWriter(
            BookResponse.class,
            BookResponse.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE);
    MessageBodyReader<BookRequest> reader =
        providers.getMessageBodyReader(
            BookRequest.class,
            BookRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE);

    return "writer (BookResponse -> JSON): "
        + describe(writer)
        + "\n"
        + "reader (JSON -> BookRequest): "
        + describe(reader);
  }

  private static String describe(Object provider) {
    return provider == null ? "none found" : provider.getClass().getName();
  }
}
