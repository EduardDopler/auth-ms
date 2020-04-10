package de.dopler.ms.jwt_server;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@RequestScoped
@Path("/auth/verify")
public class VerifyTokenResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String verify(@Context SecurityContext ctx) {
        Principal caller = ctx.getUserPrincipal();
        return jwt.getIssuer() + jwt.getClaim(Claims.upn.name()) + jwt.getName() + jwt.getGroups()
                .toString();
    }
}
