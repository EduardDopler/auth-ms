package de.dopler.ms.login_server;

import de.dopler.ms.login_server.utils.PasswordHashUtils;
import de.dopler.ms.login_server.utils.ResponseUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static de.dopler.ms.login_server.utils.TokenUtils.isUnauthorizedToChangeAdminOnlyData;
import static de.dopler.ms.login_server.utils.TokenUtils.isUnauthorizedToChangeData;

@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class UpdateCredentialsResource {

    private final AuthStoreService authStoreService;
    private final JsonWebToken jwt;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public UpdateCredentialsResource(@RestClient AuthStoreService authStoreService,
            JsonWebToken jwt) {
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
            return ResponseUtils.status(Status.FORBIDDEN);
        }

        var response = authStoreService.updateUid(id, newUid);
        if (response.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            // don't leak actual status code to client
            return ResponseUtils.fromResponse(response, Status.BAD_REQUEST);
        }
        return ResponseUtils.fromResponse(response, Status.NO_CONTENT);
    }

    @PUT
    @Path("/{id}/secret")
    public Response updateSecret(@PathParam("id") Long id, String newSecret) {
        if (newSecret == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        if (isUnauthorizedToChangeData(jwt, id)) {
            return ResponseUtils.status(Status.FORBIDDEN);
        }

        var hashedSecret = PasswordHashUtils.bcryptHash(newSecret);
        var response = authStoreService.updateSecret(id, hashedSecret);
        if (response.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            // don't leak actual status code to client
            return ResponseUtils.fromResponse(response, Status.BAD_REQUEST);
        }
        return ResponseUtils.fromResponse(response, Status.NO_CONTENT);
    }

    @PUT
    @Path("/{id}/groups")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroups(@PathParam("id") Long id, Set<String> groups) {
        if (groups == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        if (isUnauthorizedToChangeAdminOnlyData(jwt)) {
            return ResponseUtils.status(Status.FORBIDDEN);
        }

        var response = authStoreService.updateGroups(id, groups);
        if (response.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            // don't leak actual status code to client
            return ResponseUtils.fromResponse(response, Status.BAD_REQUEST);
        }
        return ResponseUtils.fromResponse(response, Status.NO_CONTENT);
    }
}