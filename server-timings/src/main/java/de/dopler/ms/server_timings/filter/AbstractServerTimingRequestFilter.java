package de.dopler.ms.server_timings.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.time.Instant;

/**
 * Request filter that reads the current time and stores it into a request property. It
 * corresponds to the {@link AbstractServerTimingResponseFilter} which finalizes the timings
 * results.
 */
public abstract class AbstractServerTimingRequestFilter implements ContainerRequestFilter {

    static final String PROPERTY_SERVER_TIMING_START = "de.dopler.ms.server_timings.start";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(PROPERTY_SERVER_TIMING_START, Instant.now());
    }
}
