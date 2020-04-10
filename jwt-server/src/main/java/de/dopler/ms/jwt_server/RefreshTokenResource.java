package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.utils.ResponseUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Path("/auth/refresh")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RefreshTokenResource {

    private final GenerateTokenService generateTokenService;
    private final JsonWebToken jwt;

    @Inject
    public RefreshTokenResource(GenerateTokenService generateTokenService, JsonWebToken jwt) {
        this.generateTokenService = generateTokenService;
        this.jwt = jwt;
    }

    @POST
    public Response fromRefreshToken() {
        if (!GenerateTokenService.SUBJECT_REFRESH.equals(jwt.getSubject())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("\"invalid subject\"")
                    .cacheControl(ResponseUtils.disableCache())
                    .build();
        }

        var jwtResponseOptional = generateTokenService.refreshJwtTokens(jwt.getTokenID());
        if (jwtResponseOptional.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("\"unknown jti\"")
                    .header(HttpHeaders.SET_COOKIE, ResponseUtils.REFRESH_TOKEN_DELETE_COOKIE)
                    .cacheControl(ResponseUtils.disableCache())
                    .build();
        }

        var jwtResponse = jwtResponseOptional.get();
        String cookie = ResponseUtils.cookieForRefreshToken(jwtResponse.refreshToken);
        return Response.ok(jwtResponse)
                .header(HttpHeaders.SET_COOKIE, cookie)
                .cacheControl(ResponseUtils.disableCache())
                .build();
    }

    @POST
    @Path("/cleanup")
    public Response cleanupExpiredTokens() {
        return Response.ok(generateTokenService.cleanupExpiredRefreshTokens())
                .cacheControl(ResponseUtils.disableCache())
                .build();
    }
}
