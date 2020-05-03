package de.dopler.ms.jwt_server.services.external;

import de.dopler.ms.jwt_server.domain.TokenData;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth/tokens")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
@Singleton
@RegisterRestClient
public interface TokenStoreService {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response store(TokenData tokenData);

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response popGroups(@PathParam("userId") long userId,
            @QueryParam("token-hash") String tokenHash);
}
