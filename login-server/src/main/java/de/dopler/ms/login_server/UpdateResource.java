package de.dopler.ms.login_server;

import de.dopler.ms.login_server.utils.PasswordHashUtils;
import de.dopler.ms.login_server.utils.ResponseUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static de.dopler.ms.login_server.utils.TokenUtils.isUnauthorizedToChangeAdminOnlyData;
import static de.dopler.ms.login_server.utils.TokenUtils.isUnauthorizedToChangeData;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateResource {

    private final AuthStoreService authStoreService;
    private final JsonWebToken jwt;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public UpdateResource(AuthStoreService authStoreService, JsonWebToken jwt) {
        this.authStoreService = authStoreService;
        this.jwt = jwt;
    }

    @PUT
    @Path("/{id}/uid")
    public Response updateUid(@PathParam("id") Long id, String newUid) {
        if (newUid == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        if (isUnauthorizedToChangeData(jwt, id)) {
            return ResponseUtils.response(Status.FORBIDDEN);
        }

        boolean success = authStoreService.updateUid(id, newUid);
        if (!success) {
            return ResponseUtils.response(Status.INTERNAL_SERVER_ERROR);
        }

        return ResponseUtils.response(Status.NO_CONTENT);
    }

    @PUT
    @Path("/{id}/secret")
    public Response updateSecret(@PathParam("id") Long id, String newSecret) {
        if (newSecret == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        if (isUnauthorizedToChangeData(jwt, id)) {
            return ResponseUtils.response(Status.FORBIDDEN);
        }

        String hashedSecret = PasswordHashUtils.bcryptHash(newSecret);
        boolean success = authStoreService.updateSecret(id, hashedSecret);
        if (!success) {
            return ResponseUtils.response(Status.INTERNAL_SERVER_ERROR);
        }

        return ResponseUtils.response(Status.NO_CONTENT);
    }

    @PUT
    @Path("/{id}/groups")
    public Response updateGroups(@PathParam("id") Long id, Set<String> groups) {
        if (groups == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        if (isUnauthorizedToChangeAdminOnlyData(jwt)) {
            return ResponseUtils.response(Status.FORBIDDEN);
        }

        boolean success = authStoreService.updateGroups(id, groups);
        if (!success) {
            return ResponseUtils.response(Status.INTERNAL_SERVER_ERROR);
        }

        return ResponseUtils.response(Status.NO_CONTENT);
    }
}
