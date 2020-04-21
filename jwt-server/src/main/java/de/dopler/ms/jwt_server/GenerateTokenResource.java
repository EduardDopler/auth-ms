package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.JwtResponse;
import de.dopler.ms.jwt_server.domain.User;
import de.dopler.ms.jwt_server.services.GenerateTokenService;
import de.dopler.ms.jwt_server.utils.ResponseUtils;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth/generate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GenerateTokenResource {

    private final GenerateTokenService generateTokenService;

    @Inject
    public GenerateTokenResource(GenerateTokenService generateTokenService) {
        this.generateTokenService = generateTokenService;
    }

    @POST
    public Response forUser(User user) {
        if (user == null || user.id == null || user.groups == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("\"invalid user object\"")
                    .cacheControl(ResponseUtils.disableCache())
                    .build();
        }

        var jwtResponse = generateTokenService.generateJwtTokens(user.id.toString(), user.groups);

        var cookie = ResponseUtils.cookieForRefreshToken(jwtResponse.refreshToken);
        // remove refresh token as it is already present in the header
        var sanitizedJwtResponse = new JwtResponse(jwtResponse.accessToken, null,
                jwtResponse.expiresAt);
        return Response.ok(sanitizedJwtResponse)
                .header(HttpHeaders.SET_COOKIE, cookie)
                .cacheControl(ResponseUtils.disableCache())
                .build();
    }
}
