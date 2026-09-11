package org.timsoft.api.ping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// WebLogic
// http://localhost:7001/sample-jee8/api/ping

@RequestScoped
@Path("ping")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "ping")
public class PingResource {
  @GET
  @Operation(
      summary = "Deployment sanity check",
      description = "Returns \"pong\" when the app is up.")
  public Response ping() {
    return Response.ok("pong").build();
  }
}
