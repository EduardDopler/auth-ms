package de.dopler.ms.response_utils;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ResponseUtils {

    private static final String SERVER_TIMING_HEADER_NAME = "Server-Timing";

    public static final String CACHE_CONTROL_ALL_OFF_STRING = "private, no-store, no-cache";
    private static final CacheControl CACHE_CONTROL_ALL_OFF = CacheControl.valueOf(
            CACHE_CONTROL_ALL_OFF_STRING);

    private ResponseUtils() {
        // utility class
    }

    /**
     * Create a new {@link Response} from the given {@code response} (keeping headers and body) and
     * override its status code with the given {@code status}. Also add all given {@code
     * serverTimingHeaderValues} and merge them into a single header value. All {@code null} values
     * are removed before setting the header. If no non-null values remain, the header will not be
     * set at all.
     * <p>
     * The {@code Cache-Control} header is set to {@value #CACHE_CONTROL_ALL_OFF_STRING}.
     * <p>
     * <b>Warning:</b> Note that the {@code response} is shallow-copied via
     * {@link Response#fromResponse(Response)}, so it must not be committed yet.
     */
    @NonNull
    public static Response fromResponse(Response response, Response.Status status,
            @Nullable String... serverTimingHeaderValues) {
        var finalResponse = Response.fromResponse(response)
                .status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF);
        var serverTimings = sanitizeTimingValues(serverTimingHeaderValues);
        if (serverTimings == null) {
            return finalResponse.build();
        }
        return finalResponse.header(SERVER_TIMING_HEADER_NAME, serverTimings).build();
    }

    /**
     * Create a new {@link Response} with the given {@code status}, {@code mediaType} and {@code
     * body}. If the given {@code cookie} is non-null, add that, too.
     * <p>
     * Also add all given {@code serverTimingHeaderValues} and merge them into a single header
     * value. All {@code null} values are removed before setting the header. If no non-null
     * values remain, the header will not be set at all.
     * <p>
     * The {@code Cache-Control} header is set to {@value #CACHE_CONTROL_ALL_OFF_STRING}.
     */
    @NonNull
    public static Response response(Response.Status status, @NonNull MediaType type,
            @NonNull Object body, @Nullable RefreshTokenCookie cookie,
            @Nullable String... serverTimingHeaderValues) {
        var response = Response.status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .type(type)
                .entity(body);
        if (cookie != null) {
            response.header(HttpHeaders.SET_COOKIE, cookie);
        }
        var serverTimings = sanitizeTimingValues(serverTimingHeaderValues);
        if (serverTimings == null) {
            return response.build();
        }
        return response.header(SERVER_TIMING_HEADER_NAME, serverTimings).build();
    }

    /**
     * Same as {@link #response(Response.Status, MediaType, Object, RefreshTokenCookie, String...)}
     * but without a {@code cookie}.
     */
    @NonNull
    public static Response response(Response.Status status, @NonNull MediaType type,
            @NonNull Object body, @Nullable String... serverTimingHeaderValues) {
        return response(status, type, body, null, serverTimingHeaderValues);
    }

    /**
     * Same as {@link #jsonResponse(Response.Status, Object, RefreshTokenCookie, String...)}
     * but without a {@code cookie}.
     */
    @NonNull
    public static Response jsonResponse(Response.Status status, @NonNull Object body,
            @Nullable String... serverTimingHeaderValues) {
        return jsonResponse(status, body, null, serverTimingHeaderValues);
    }

    /**
     * Same as {@link #response(Response.Status, MediaType, Object, RefreshTokenCookie, String...)}
     * with the media type set to {@link MediaType#TEXT_PLAIN_TYPE}.
     */
    @NonNull
    public static Response jsonResponse(Response.Status status, @NonNull Object body,
            @Nullable RefreshTokenCookie cookie, @Nullable String... serverTimingHeaderValues) {
        return response(status, MediaType.APPLICATION_JSON_TYPE, body, cookie,
                serverTimingHeaderValues);
    }

    /**
     * Same as {@link #textResponse(Response.Status, CharSequence, RefreshTokenCookie, String...)}
     * but without a {@code cookie}.
     */
    @NonNull
    public static Response textResponse(Response.Status status, @NonNull CharSequence body,
            @Nullable String... serverTimingHeaderValues) {
        return textResponse(status, body, null, serverTimingHeaderValues);
    }

    /**
     * Same as {@link #response(Response.Status, MediaType, Object, RefreshTokenCookie, String...)}
     * with the media type set to {@link MediaType#TEXT_PLAIN_TYPE}.
     */
    @NonNull
    public static Response textResponse(Response.Status status, @NonNull CharSequence body,
            @Nullable RefreshTokenCookie cookie, @Nullable String... serverTimingHeaderValues) {
        return response(status, MediaType.TEXT_PLAIN_TYPE, body, cookie, serverTimingHeaderValues);
    }

    /**
     * Same as {@link #status(Response.Status, RefreshTokenCookie)} but without a {@code cookie}.
     */
    @NonNull
    public static Response status(Response.Status status) {
        return status(status, null);
    }

    /**
     * Create a new {@link Response} with the given {@code status} and {@code cookie} (if non-null).
     */
    @NonNull
    public static Response status(Response.Status status, @Nullable RefreshTokenCookie cookie) {
        var response = Response.status(status).cacheControl(CACHE_CONTROL_ALL_OFF);
        if (cookie != null) {
            response.header(HttpHeaders.SET_COOKIE, cookie);
        }
        return response.build();
    }

    @Nullable
    private static String sanitizeTimingValues(@Nullable String... timingValues) {
        if (timingValues == null || timingValues.length == 0) {
            return null;
        }
        var nonNullTimingValues = Arrays.stream(timingValues)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return nonNullTimingValues.length() == 0 ? null : nonNullTimingValues;
    }
}
