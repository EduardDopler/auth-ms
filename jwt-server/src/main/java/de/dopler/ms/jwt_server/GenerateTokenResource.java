package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.JwtResponse;
import de.dopler.ms.jwt_server.domain.TokenData;
import de.dopler.ms.jwt_server.domain.User;
import de.dopler.ms.jwt_server.services.external.TokenStoreService;
import de.dopler.ms.jwt_server.utils.GenerateTokenUtils;
import de.dopler.ms.jwt_server.utils.RefreshTokenUtils;
import de.dopler.ms.response_utils.RefreshTokenCookie;
import de.dopler.ms.response_utils.ResponseUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static de.dopler.ms.jwt_server.utils.GenerateTokenUtils.EXPIRATION_REFRESH_TOKEN;
import static de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter.SERVER_TIMING_HEADER_NAME;

@Path("/auth/generate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GenerateTokenResource {

    private final TokenStoreService tokenStoreService;

    @Inject
    public GenerateTokenResource(@RestClient TokenStoreService tokenStoreService) {
        this.tokenStoreService = tokenStoreService;
    }

    @POST
    public Response forUser(User user) {
        if (user == null || user.groups == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "invalid user object");
        }

        var tokens = GenerateTokenUtils.generateJwtTokens(user.id, user.groups);

        var tokenHash = RefreshTokenUtils.toSha256Hash(tokens.refreshToken);
        var tokenData = new TokenData(user.id, tokenHash, user.groups,
                tokens.refreshTokenExpiresAt);
        var storedTokenResponse = tokenStoreService.store(tokenData);
        var tokenStoreTiming = storedTokenResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);

        var cookie = new RefreshTokenCookie(tokens.refreshToken, EXPIRATION_REFRESH_TOKEN);
        var jwtResponse = new JwtResponse(tokens.accessToken, tokens.accessTokenExpiresAt);

        return ResponseUtils.jsonResponse(Status.OK, jwtResponse, cookie, tokenStoreTiming);
    }
}
