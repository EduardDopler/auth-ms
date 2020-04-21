package de.dopler.ms.login_server;

import de.dopler.ms.login_server.domain.AuthData;
import de.dopler.ms.login_server.domain.Credentials;
import de.dopler.ms.login_server.domain.User;
import de.dopler.ms.login_server.utils.PasswordHashUtils;
import de.dopler.ms.login_server.utils.ResponseUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;

import static de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter.SERVER_TIMING_HEADER_NAME;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {

    private static final int DELAY_CREDENTIALS_MISMATCH_MILLIS = 3000;
    private static final String RESPONSE_TEXT_CREDENTIALS_MISMATCH = "credentials mismatch";

    private final AuthStoreService authStoreService;
    private final TokenService tokenService;

    @Inject
    public LoginResource(@RestClient AuthStoreService authStoreService,
            @RestClient TokenService tokenService) {
        this.authStoreService = authStoreService;
        this.tokenService = tokenService;
    }

    @POST
    @Path("/register")
    @Produces(MediaType.TEXT_PLAIN)
    public Response register(@QueryParam("no-login") boolean noLogin, Credentials credentials) {
        if (credentials == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }

        var hashedSecret = PasswordHashUtils.bcryptHash(credentials.secret);
        var idResponse = authStoreService.storeCredentials(
                new Credentials(credentials.uid, hashedSecret));
        var timingCredentials = idResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);

        if (idResponse.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
            return ResponseUtils.fromResponse(idResponse, idResponse.getStatusInfo().toEnum());
        }

        if (noLogin) {
            return ResponseUtils.fromResponse(idResponse, Status.OK);
        }
        var id = idResponse.readEntity(Long.TYPE);

        // retrieve token
        var user = new User(id, Collections.emptySet());
        var tokenResponse = tokenService.forUser(user);
        return ResponseUtils.fromResponse(tokenResponse, Status.OK, timingCredentials);
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Credentials credentials) {
        if (credentials == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }

        // check credentials
        var authDataResponse = authStoreService.getAuthData(credentials.uid);
        var timingCredentials = authDataResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);

        if (authDataResponse.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR) {
            return ResponseUtils.fromResponse(authDataResponse, Status.INTERNAL_SERVER_ERROR);
        }
        if (authDataResponse.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            return ResponseUtils.fromResponse(authDataResponse, Status.UNAUTHORIZED);
        }
        var authData = authDataResponse.readEntity(AuthData.class);

        if (!PasswordHashUtils.verify(authData.secret, credentials.secret)) {
            delayResponse();
            return ResponseUtils.textResponse(Status.UNAUTHORIZED,
                    RESPONSE_TEXT_CREDENTIALS_MISMATCH, timingCredentials);
        }

        // retrieve token
        var user = new User(authData.id, authData.groups);
        var tokenResponse = tokenService.forUser(user);

        if (authDataResponse.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            // as we checked the credentials already, this can only be an error on our side
            return ResponseUtils.fromResponse(tokenResponse, Status.INTERNAL_SERVER_ERROR,
                    timingCredentials);
        }

        return ResponseUtils.fromResponse(tokenResponse, Status.OK, timingCredentials);
    }

    private static void delayResponse() {
        try {
            Thread.sleep(DELAY_CREDENTIALS_MISMATCH_MILLIS);
        } catch (InterruptedException e) {
            // don't propagate; the program can continue running in case of an InterruptedException
        }
    }
}
