package de.dopler.ms.login_server.services.external;

import de.dopler.ms.login_server.domain.Credentials;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/auth/credentials")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
@Singleton
@RegisterRestClient
public interface CredentialsStoreService {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response storeCredentials(Credentials credentials);

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getAuthData(@PathParam("username") String username);

    @PUT
    @Path("/{id}/username")
    Response updateUsername(@PathParam("id") long id, String newUsername);

    @PUT
    @Path("/{id}/secret")
    Response updateSecret(@PathParam("id") long id, String newSecret);

    @PUT
    @Path("/{id}/groups")
    Response updateGroups(@PathParam("id") long id, Set<String> newGroups);
}
