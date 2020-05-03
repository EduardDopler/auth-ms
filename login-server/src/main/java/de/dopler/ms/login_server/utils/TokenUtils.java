package de.dopler.ms.login_server.utils;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

public final class TokenUtils {

    private static final Logger LOG = Logger.getLogger("TokenUtils");

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private TokenUtils() {
        // utility class
    }

    /**
     * Check if access is forbidden for the given JWT and {@code id}. It is forbidden, if
     * <ul>
     *     <li>the {@code jwt} or its {@link JsonWebToken#getName()} (the upn claim) is null,</li>
     *     <li>the upn claim cannot be parsed as a Long or</li>
     *     <li>the upn claim does not equal the given {@code id} EXCEPT the JWT's groups claims
     *     contain the role {@link #ROLE_ADMIN}.</li>
     * </ul>
     */
    public static boolean isUnauthorizedToChangeData(@Nullable JsonWebToken jwt, long id) {
        if (jwt == null || jwt.getName() == null) {
            return true;
        }
        Long jwtUpn;
        try {
            jwtUpn = Long.valueOf(jwt.getName());
        } catch (NumberFormatException e) {
            LOG.warn("Cannot convert upn claim in this JWT to Long");
            return true;
        }
        return !jwtUpn.equals(id) && !jwt.getGroups().contains(ROLE_ADMIN);
    }

    /**
     * Check if access is forbidden for the given {@code jwt} JWT. It is forbidden, if
     * <ul>
     *     <li>the {@code jwt} or its {@link JsonWebToken#getName()} (the upn claim) is null or</li>
     *     <li>the JWT's groups claims don't contain the role {@link #ROLE_ADMIN}.</li>
     * </ul>
     */
    public static boolean isUnauthorizedToChangeAdminOnlyData(@Nullable JsonWebToken jwt) {
        return jwt == null || jwt.getName() == null || !jwt.getGroups().contains(ROLE_ADMIN);
    }
}
