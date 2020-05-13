package de.dopler.ms.token_store;

import de.dopler.ms.response_utils.ResponseUtils;
import de.dopler.ms.token_store.domain.TokenData;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/auth/tokens")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class TokenStoreResource {

    private final TokenStoreService tokenStoreService;

    @Inject
    public TokenStoreResource(TokenStoreService tokenStoreService) {
        this.tokenStoreService = tokenStoreService;
    }

    @PUT
    @Path("/db-init")
    public Response initStore() {
        try {
            tokenStoreService.initStore();
        } catch (IllegalStateException e) {
            return ResponseUtils.textResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ResponseUtils.status(Status.NO_CONTENT);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeToken(TokenData tokenData) {
        if (tokenData == null || tokenData.tokenHash == null || tokenData.groups == null) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "invalid token object");
        }

        boolean stored;
        try {
            stored = tokenStoreService.put(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                    tokenData.expiresAt);
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return stored ?
                ResponseUtils.status(Status.NO_CONTENT) :
                ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
    }

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response popGroups(@PathParam("userId") long userId,
            @QueryParam("token-hash") String tokenHash) {
        if (tokenHash == null || tokenHash.length() == 0) {
            return ResponseUtils.textResponse(Status.BAD_REQUEST, "missing token hash");
        }

        try {
            return tokenStoreService.popGroups(userId, tokenHash)
                    .map(groups -> Response.ok(groups).build())
                    .orElse(Response.status(Status.NOT_FOUND).build());
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/expired")
    public Response deleteExpired() {
        int deleted;
        try {
            deleted = tokenStoreService.deleteExpired();
        } catch (IllegalStateException e) {
            return ResponseUtils.status(Status.INTERNAL_SERVER_ERROR);
        }
        return ResponseUtils.textResponse(Status.OK, String.valueOf(deleted));
    }
}
