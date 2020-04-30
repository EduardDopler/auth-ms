package de.dopler.ms.token_store.filter;

import de.dopler.ms.server_timings.filter.AbstractServerTimingResponseFilter;

import javax.ws.rs.ext.Provider;

@Provider
public class ServerTimingResponseFilter extends AbstractServerTimingResponseFilter {

    private static final String MICROSERVICE_TIMING_ID = "t";
    private static final String SERVER_TIMING_KEY = MICROSERVICE_TIMING_ID + ";dur=";

    @Override
    protected String serverTimingKey() {
        return SERVER_TIMING_KEY;
    }
}
