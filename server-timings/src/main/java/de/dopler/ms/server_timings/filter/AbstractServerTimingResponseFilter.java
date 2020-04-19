package de.dopler.ms.server_timings.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static de.dopler.ms.server_timings.filter.AbstractServerTimingRequestFilter.PROPERTY_SERVER_TIMING_START;

/**
 * Response filter that reads the start time measured in {@link AbstractServerTimingRequestFilter}
 * (written into the {@value AbstractServerTimingRequestFilter#PROPERTY_SERVER_TIMING_START}
 * property) and writes the final duration for this microservice into the {@code Server-Timing}
 * header of the response.
 * <p>
 * Each microservice defines its own key by overriding {@link #serverTimingKey()} which is used for
 * the timing identifier (in spec's terms the "server-timing-param-name").
 * <p>
 * If one or multiple {@code Server-Timing} headers are already present, they are merged with the
 * current timing, following the Server-Timing spec in
 * https://w3c.github.io/server-timing/#the-server-timing-header-field.
 */
public abstract class AbstractServerTimingResponseFilter implements ContainerResponseFilter {

    public static final String SERVER_TIMING_HEADER_NAME = "Server-Timing";

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        var timingStart = (Instant) requestContext.getProperty(PROPERTY_SERVER_TIMING_START);
        if (timingStart == null) {
            return;
        }
        var duration = timingStart.until(Instant.now(), ChronoUnit.MILLIS);

        var responseHeaders = responseContext.getHeaders();
        var serverTimingKey = serverTimingKey();
        if (responseHeaders.containsKey(SERVER_TIMING_HEADER_NAME)) {
            var oldValues = responseContext.getStringHeaders().get(SERVER_TIMING_HEADER_NAME);
            var combinedOldValues = String.join(", ", oldValues);
            var newValue = String.format("%s, %s%d", combinedOldValues, serverTimingKey, duration);
            responseHeaders.putSingle(SERVER_TIMING_HEADER_NAME, newValue);
        } else {
            responseHeaders.putSingle(SERVER_TIMING_HEADER_NAME, serverTimingKey + duration);
        }
    }

    protected abstract String serverTimingKey();
}
