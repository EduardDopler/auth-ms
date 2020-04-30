package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.JwtResponse;
import de.dopler.ms.jwt_server.domain.TokenData;
import de.dopler.ms.jwt_server.services.external.TokenStoreService;
import de.dopler.ms.jwt_server.utils.GenerateTokenUtils;
import de.dopler.ms.jwt_server.utils.RefreshTokenUtils;
import de.dopler.ms.response_utils.RefreshTokenCookie;
import de.dopler.ms.response_utils.ResponseUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter.SERVER_TIMING_HEADER_NAME;

@RequestScoped
@Path("/auth/refresh")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RefreshTokenResource {

    private final TokenStoreService tokenStoreService;
    private final JsonWebToken jwt;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public RefreshTokenResource(@RestClient TokenStoreService tokenStoreService, JsonWebToken jwt) {
        this.tokenStoreService = tokenStoreService;
        this.jwt = jwt;
    }

    @POST
    public Response fromRefreshToken() {
        if (!GenerateTokenUtils.SUBJECT_REFRESH.equals(jwt.getSubject())) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "invalid subject");
        }

        long userId;
        try {
            userId = Long.parseLong(jwt.getName());
        } catch (NumberFormatException e) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST,
                    "userId inside upn cannot be parsed");
        }

        var tokenHash = RefreshTokenUtils.toSha256Hash(jwt.getRawToken());
        var groupsResponse = tokenStoreService.popGroups(userId, tokenHash);
        var timing = groupsResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);

        if (groupsResponse.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR) {
            var deleteCookie = new RefreshTokenCookie("", 0);
            return ResponseUtils.status(Status.BAD_REQUEST, deleteCookie);
        }
        if (groupsResponse.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        var groups = groupsResponse.readEntity(new GenericType<Set<String>>() {});

        var tokens = GenerateTokenUtils.generateJwtTokens(userId, groups);
        var newTokenHash = RefreshTokenUtils.toSha256Hash(tokens.refreshToken);

        var tokenData = new TokenData(userId, newTokenHash, groups, tokens.refreshTokenExpiresAt);
        var storedTokenResponse = tokenStoreService.store(tokenData);
        var tokenStoreTiming = storedTokenResponse.getHeaderString(SERVER_TIMING_HEADER_NAME);

        var cookie = new RefreshTokenCookie(tokens.refreshToken, tokens.refreshTokenExpiresAt);
        var jwtResponse = new JwtResponse(tokens.accessToken, tokens.accessTokenExpiresAt);

        return ResponseUtils.jsonResponse(Status.OK, jwtResponse, cookie, timing, tokenStoreTiming);
    }
}
