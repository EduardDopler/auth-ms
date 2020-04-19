package de.dopler.ms.login_server.utils;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter.SERVER_TIMING_HEADER_NAME;

public final class ResponseUtils {

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
        var serverTimings = sanitizeTimingValues(serverTimingHeaderValues);
        if (serverTimings == null) {
            return fromResponse(response, status);
        }
        return Response.fromResponse(response)
                .status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .header(SERVER_TIMING_HEADER_NAME, serverTimings)
                .build();
    }

    /**
     * Like {@link ResponseUtils#fromResponse(Response, Response.Status, String...)} but without the
     * server timing parameter(s).
     */
    @NonNull
    public static Response fromResponse(Response response, Response.Status status) {
        return Response.fromResponse(response)
                .status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .build();
    }

    /**
     * Create a new {@link Response} with the given {@code status}. Also add all given {@code
     * serverTimingHeaderValues} and merge them into a single header value. All {@code null} values
     * are removed before setting the header. If no non-null values remain, the header will not be
     * set at all.
     * <p>
     * The {@code Cache-Control} header is set to {@value #CACHE_CONTROL_ALL_OFF_STRING}.
     */
    @NonNull
    public static Response response(Response.Status status, @NonNull MediaType type,
            @NonNull Object body, @Nullable String... serverTimingHeaderValues) {
        var serverTimings = sanitizeTimingValues(serverTimingHeaderValues);
        if (serverTimings == null) {
            return response(status, type, body);
        }
        return Response.status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .header(SERVER_TIMING_HEADER_NAME, serverTimings)
                .type(type)
                .entity(body)
                .build();
    }

    /**
     * Like {@link ResponseUtils#response(Response.Status, MediaType, Object, String...)} but
     * without the server timing parameter(s).
     */
    @NonNull
    public static Response response(Response.Status status, @NonNull MediaType type,
            @NonNull Object body) {
        return Response.status(status)
                .cacheControl(CACHE_CONTROL_ALL_OFF)
                .type(type)
                .entity(body)
                .build();
    }

    /**
     * Same as {@link #response(Response.Status, MediaType, Object, String...)} with the media type
     * set to {@link MediaType#TEXT_PLAIN_TYPE}.
     */
    @NonNull
    public static Response textResponse(Response.Status status, @NonNull CharSequence body,
            @Nullable String... serverTimingHeaderValues) {
        return response(status, MediaType.TEXT_PLAIN_TYPE, body, serverTimingHeaderValues);
    }

    /**
     * Create a new {@link Response} with the given {@code status} and the {@code Cache-Control}
     * header set to {@value #CACHE_CONTROL_ALL_OFF_STRING}.
     */
    @NonNull
    public static Response status(Response.Status status) {
        return Response.status(status).cacheControl(CACHE_CONTROL_ALL_OFF).build();
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
