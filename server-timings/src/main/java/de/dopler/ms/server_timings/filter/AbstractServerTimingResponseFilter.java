package de.dopler.ms.server_timings.filter;

import io.vertx.core.http.HttpServerResponse;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import static de.dopler.ms.server_timings.filter.AbstractServerTimingRequestFilter.SERVER_TIMING_INTERNAL_HEADER_NAME;

/**
 * Response filter that reads the start time measured in {@link AbstractServerTimingRequestFilter}
 * (written into the
 * {@value AbstractServerTimingRequestFilter#SERVER_TIMING_INTERNAL_HEADER_NAME} header) and
 * writes the final duration for this microservice into the {@code Server-Timing} header of the
 * response.
 * <p>
 * If a {@code Server-Timing} header is already present, it is merged with the timing, according
 * to the Server-Timing spec in https://w3c.github.io/server-timing/#the-server-timing-header-field.
 */
public abstract class AbstractServerTimingResponseFilter implements ContainerResponseFilter {

    private static final String SERVER_TIMING_RESPONSE_HEADER_NAME = "Server-Timing";

    @Context
    HttpServerResponse response;

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        // response.headers() contains only the current response's headers (e.g. set by filters)
        var headers = response.headers();
        if (!headers.contains(SERVER_TIMING_INTERNAL_HEADER_NAME)) {
            return;
        }
        var serverTiming = headers.get(SERVER_TIMING_INTERNAL_HEADER_NAME);
        var duration = 0L;
        try {
            duration = Instant.parse(serverTiming).until(Instant.now(), ChronoUnit.MILLIS);
        } catch (DateTimeParseException e) {
            // ignore
        }
        headers.remove(SERVER_TIMING_INTERNAL_HEADER_NAME);

        // responseContext.getHeaders() contains all headers, also the ones from responses committed
        // by other services
        var contextHeaders = responseContext.getHeaders();
        var serverTimingKey = serverTimingKey();
        if (contextHeaders.containsKey(SERVER_TIMING_RESPONSE_HEADER_NAME)) {
            var oldValue = contextHeaders.getFirst(SERVER_TIMING_RESPONSE_HEADER_NAME);
            var newValue = String.format("%s, %s%d", oldValue, serverTimingKey, duration);
            contextHeaders.putSingle(SERVER_TIMING_RESPONSE_HEADER_NAME, newValue);
        } else {
            headers.add(SERVER_TIMING_RESPONSE_HEADER_NAME, serverTimingKey + duration);
        }
    }

    protected abstract String serverTimingKey();
}
