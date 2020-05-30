package de.dopler.ms.login_server.services.external;

import de.dopler.ms.login_server.domain.User;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
@RegisterRestClient
public interface TokenService {

    @POST
    @Path("/generate")
    Response forUser(User user);

    @POST
    @Path("/refresh")
    Response fromRefreshToken(@CookieParam("r_token") String refreshToken);
}
