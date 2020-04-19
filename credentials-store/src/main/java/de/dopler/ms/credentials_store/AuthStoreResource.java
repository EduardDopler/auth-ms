package de.dopler.ms.credentials_store;

import de.dopler.ms.credentials_store.domain.Credentials;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

@Path("/auth/credentials")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class AuthStoreResource {

    private final AuthStoreService authStoreService;

    @Inject
    public AuthStoreResource(AuthStoreService authStoreService) {
        this.authStoreService = authStoreService;
    }

    @PUT
    @Path("/db-init")
    public Response initStore() {
        try {
            authStoreService.initStore();
        } catch (IllegalStateException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeCredentials(Credentials credentials) {
        try {
            return authStoreService.storeCredentials(credentials.uid, credentials.secret)
                    .map(id -> Response.ok(id).build())
                    .orElse(Response.serverError().build());
        } catch (IllegalArgumentException e) {
            return Response.status(Status.CONFLICT).build();
        }
    }

    @GET
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthData(@PathParam("uid") String uid) {
        try {
            return authStoreService.getAuthData(uid)
                    .map(authData -> Response.ok(authData).build())
                    .orElse(Response.status(Status.NOT_FOUND).build());
        } catch (IllegalStateException e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("/{id}/uid")
    public Response updateUid(@PathParam("id") Long id, String newUid) {
        boolean updated;
        try {
            updated = authStoreService.updateUid(id, newUid);
        } catch (IllegalStateException e) {
            return Response.serverError().build();
        }
        return updated ? Response.noContent().build() : Response.status(Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{id}/secret")
    public Response updateSecret(@PathParam("id") Long id, String newSecret) {
        boolean updated;
        try {
            updated = authStoreService.updateSecret(id, newSecret);
        } catch (IllegalStateException e) {
            return Response.serverError().build();
        }
        return updated ? Response.noContent().build() : Response.status(Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{id}/groups")
    public Response updateGroups(@PathParam("id") Long id, Set<String> newGroups) {
        boolean updated;
        try {
            updated = authStoreService.updateGroups(id, newGroups);
        } catch (IllegalStateException e) {
            return Response.serverError().build();
        }
        return updated ? Response.noContent().build() : Response.status(Status.NOT_FOUND).build();
    }
}
