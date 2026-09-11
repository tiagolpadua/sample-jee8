package org.timsoft.api.json;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import javax.ws.rs.ext.Provider;

/**
 * Registers Jackson as a {@code MessageBodyReader}/{@code MessageBodyWriter} for JSON.
 *
 * <p>WebLogic 14.1.2's Jersey has Jackson, MOXy, and JSON-B all on its classpath at once and, by
 * default, picks JSON-B (confirmed via {@code diagnostics.DiagnosticsResource} - see the "JSON
 * serialization" section of CLAUDE.md). An {@code @Provider} class the container discovers in this
 * WAR outranks the providers Jersey auto-discovers on its own, so a trivial subclass here is enough
 * to make this one win - no need to touch {@code ApplicationConfig.getClasses()}.
 *
 * <p>{@code ApplicationConfig.getProperties()} also explicitly disables JSON-B and MOXy, so there
 * is no ambiguity even if that ranking behavior ever changes.
 */
@Provider
public class AppJacksonJsonProvider extends JacksonJsonProvider {}
