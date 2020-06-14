package de.dopler.ms.login_server.services.external;

import de.dopler.ms.login_server.domain.Credentials;
import org.eclipse.microprofile.faulttolerance.Retry;
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
    @Retry(maxRetries = 1, delay = 3000)
    Response storeCredentials(Credentials credentials);

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 1, delay = 3000)
    Response getAuthData(@PathParam("username") String username);

    @DELETE
    @Path("/{id}")
    @Retry(maxRetries = 1, delay = 3000)
    Response removeCredentials(@PathParam("id") long id);

    @PUT
    @Path("/{id}/username")
    @Retry(maxRetries = 1, delay = 3000)
    Response updateUsername(@PathParam("id") long id, String newUsername);

    @PUT
    @Path("/{id}/secret")
    @Retry(maxRetries = 1, delay = 3000)
    Response updateSecret(@PathParam("id") long id, String newSecret);

    @PUT
    @Path("/{id}/groups")
    @Retry(maxRetries = 1, delay = 3000)
    Response updateGroups(@PathParam("id") long id, Set<String> newGroups);
}
