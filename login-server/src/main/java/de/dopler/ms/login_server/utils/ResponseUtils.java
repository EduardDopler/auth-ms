package de.dopler.ms.login_server.utils;

import org.eclipse.jdt.annotation.NonNull;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public final class ResponseUtils {

    private static final CacheControl CACHE_CONTROL_ALL_OFF = CacheControl.valueOf(
            "private, no-store, no-cache");

    private ResponseUtils() {
        // utility class
    }

    @NonNull
    public static Response response(Response.Status status) {
        return Response.status(status).cacheControl(CACHE_CONTROL_ALL_OFF).build();
    }

    @NonNull
    public static Response response(Response.Status status, @NonNull MediaType type,
            @NonNull Object body) {
        return Response.status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .type(type)
                .entity(body)
                .build();
    }

    @NonNull
    public static Response textResponse(Response.Status status, @NonNull CharSequence body) {
        return response(status, MediaType.TEXT_PLAIN_TYPE, body);
    }
}
