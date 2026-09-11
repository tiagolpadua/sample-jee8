package org.timsoft.api.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// WebLogic
// http://localhost:7001/sample-jee8/api/test

@RequestScoped
@Path("test")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "test")
public class TestResource {
  @GET
  @Operation(
      summary = "Deployment sanity check",
      description = "Returns \"Test OK\" when the app is up.")
  public Response test() {
    return Response.ok("Test OK").build();
  }
}
