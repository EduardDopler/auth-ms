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
import java.util.Optional;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {

    private final AuthStoreService authStoreService;
    private final TokenService tokenService;

    @Inject
    public LoginResource(AuthStoreService authStoreService, @RestClient TokenService tokenService) {
        this.authStoreService = authStoreService;
        this.tokenService = tokenService;
    }

    @PUT
    @Path("/db-init")
    public Response init() {
        try {
            authStoreService.initStore();
        } catch (IllegalStateException e) {
            return ResponseUtils.textResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ResponseUtils.response(Status.NO_CONTENT);
    }

    @POST
    @Path("/register")
    public Response register(@QueryParam("no-login") boolean noLogin, Credentials credentials) {
        if (credentials == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        String hashedSecret = PasswordHashUtils.bcryptHash(credentials.secret);
        Optional<Long> id;
        try {
            id = authStoreService.storeCredentials(credentials.uid, hashedSecret);
        } catch (IllegalArgumentException e) {
            return ResponseUtils.response(Status.CONFLICT);
        }
        if (id.isEmpty()) {
            return ResponseUtils.response(Status.INTERNAL_SERVER_ERROR);
        }
        if (noLogin) {
            return ResponseUtils.textResponse(Status.CREATED, id.map(String::valueOf).orElse(""));
        }
        // retrieve token
        var user = new User(id.get(), Collections.emptySet());
        return tokenService.forUser(user);
    }

    @POST
    @Path("/login")
    public Response login(Credentials credentials) {
        if (credentials == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "body has to be non-null");
        }
        Optional<AuthData> authDataOptional;
        try {
            authDataOptional = authStoreService.getId(credentials.uid);
        } catch (IllegalStateException e) {
            return ResponseUtils.response(Status.INTERNAL_SERVER_ERROR);
        }
        if (authDataOptional.isEmpty()) {
            return ResponseUtils.response(Status.UNAUTHORIZED);
        }
        var authData = authDataOptional.get();
        if (!PasswordHashUtils.verify(authData.secret, credentials.secret)) {
            return ResponseUtils.response(Status.UNAUTHORIZED);
        }
        // retrieve token
        var user = new User(authData.id, authData.groups);
        return tokenService.forUser(user);
    }
}
