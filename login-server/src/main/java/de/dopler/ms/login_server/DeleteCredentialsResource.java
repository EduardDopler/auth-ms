package de.dopler.ms.login_server;

import de.dopler.ms.login_server.services.external.CredentialsStoreService;
import de.dopler.ms.login_server.services.external.TokenService;
import de.dopler.ms.response_utils.ResponseUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static de.dopler.ms.login_server.utils.TokenUtils.isUnauthorizedToChangeAdminOnlyData;
import static de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter.SERVER_TIMING_HEADER_NAME;

@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class DeleteCredentialsResource {

    private final CredentialsStoreService credentialsStoreService;
    private final TokenService tokenService;
    private final JsonWebToken jwt;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public DeleteCredentialsResource(@RestClient CredentialsStoreService credentialsStoreService,
            @RestClient TokenService tokenService, JsonWebToken jwt) {
        this.credentialsStoreService = credentialsStoreService;
        this.tokenService = tokenService;
        this.jwt = jwt;
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        if (isUnauthorizedToChangeAdminOnlyData(jwt)) {
            return ResponseUtils.status(Status.FORBIDDEN);
        }

        var credentialsResponse = credentialsStoreService.removeCredentials(id);
        var timingCredentials = credentialsResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);
        if (credentialsResponse.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            // don't leak actual status code to client (404 Not Found)
            return ResponseUtils.fromResponse(credentialsResponse, Status.BAD_REQUEST);
        }

        var tokensResponse = tokenService.removeTokens(id);
        var timingTokens = tokensResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);
        return ResponseUtils.textResponse(Status.NO_CONTENT, "", timingCredentials, timingTokens);
    }
}
