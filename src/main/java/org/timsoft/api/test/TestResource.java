package org.timsoft.api.test;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// WildFly
// http://localhost:8080/sample-jee7/api/test

// Weblogic
// http://localhost:7001/sample-jee7/api/test

@RequestScoped
@Path("test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {
	@GET
	public Response test() {
		return Response.ok("Test OK").build();
	}
}