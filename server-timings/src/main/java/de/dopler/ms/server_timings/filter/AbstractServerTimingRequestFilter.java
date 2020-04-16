package de.dopler.ms.server_timings.filter;

import io.vertx.core.http.HttpServerRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import java.time.Instant;

/**
 * Request filter that reads the current time and stores it into a (temporary) header in the
 * response. It corresponds to the {@link AbstractServerTimingResponseFilter} which finalizes the
 * timings results and clears this header ({@value #SERVER_TIMING_INTERNAL_HEADER_NAME}).
 */
public abstract class AbstractServerTimingRequestFilter implements ContainerRequestFilter {

    static final String SERVER_TIMING_INTERNAL_HEADER_NAME = "X-Server-Timing";

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        request.response().putHeader(SERVER_TIMING_INTERNAL_HEADER_NAME, Instant.now().toString());
    }
}
