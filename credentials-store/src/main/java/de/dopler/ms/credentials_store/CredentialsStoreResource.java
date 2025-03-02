package de.dopler.ms.credentials_store;

import de.dopler.ms.credentials_store.domain.Credentials;
import de.dopler.ms.response_utils.ResponseUtils;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

@Path("/auth/credentials")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class CredentialsStoreResource {

    private final CredentialsStoreService credentialsStoreService;

    @Inject
    public CredentialsStoreResource(CredentialsStoreService credentialsStoreService) {
        this.credentialsStoreService = credentialsStoreService;
    }

    void onStart(@Observes StartupEvent ev) {
        credentialsStoreService.initStore();
    }

    @PUT
    @Path("/db-init")
    public Response initStore() {
        try {
            credentialsStoreService.initStore();
        } catch (IllegalStateException e) {
            return ResponseUtils.textResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ResponseUtils.status(Status.NO_CONTENT);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeCredentials(Credentials credentials) {
        if (credentials == null || credentials.username == null || credentials.secret == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "invalid credentials objects");
        }
        try {
            return credentialsStoreService.storeCredentials(credentials.username,
                    credentials.secret)
                    .map(id -> Response.ok(id).build())
                    .orElse(Response.serverError().build());
        } catch (IllegalArgumentException e) {
            return ResponseUtils.status(Status.CONFLICT);
        }
    }

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthData(@PathParam("username") String username) {
        try {
            return credentialsStoreService.getAuthData(username)
                    .map(authData -> Response.ok(authData).build())
                    .orElse(Response.status(Status.NOT_FOUND).build());
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response removeCredentials(@PathParam("id") long id) {
        boolean deleted;
        try {
            deleted = credentialsStoreService.removeCredentials(id);
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return deleted ?
                ResponseUtils.status(Status.NO_CONTENT) :
                ResponseUtils.status(Status.NOT_FOUND);
    }

    @PUT
    @Path("/{id}/username")
    public Response updateUsername(@PathParam("id") long id, String newUsername) {
        if (newUsername == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        boolean updated;
        try {
            updated = credentialsStoreService.updateUsername(id, newUsername);
        } catch (IllegalArgumentException e) {
            return ResponseUtils.status(Status.CONFLICT);
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return updated ?
                ResponseUtils.status(Status.NO_CONTENT) :
                ResponseUtils.status(Status.NOT_FOUND);
    }

    @PUT
    @Path("/{id}/secret")
    public Response updateSecret(@PathParam("id") long id, String newSecret) {
        if (newSecret == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        boolean updated;
        try {
            updated = credentialsStoreService.updateSecret(id, newSecret);
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return updated ?
                ResponseUtils.status(Status.NO_CONTENT) :
                ResponseUtils.status(Status.NOT_FOUND);
    }

    @PUT
    @Path("/{id}/groups")
    public Response updateGroups(@PathParam("id") long id, Set<String> newGroups) {
        if (newGroups == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        boolean updated;
        try {
            updated = credentialsStoreService.updateGroups(id, newGroups);
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return updated ?
                ResponseUtils.status(Status.NO_CONTENT) :
                ResponseUtils.status(Status.NOT_FOUND);
    }
}
