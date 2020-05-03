package de.dopler.ms.server_timings.filter;

import org.jboss.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final Logger LOG = Logger.getLogger("ServerTimingResponseFilter");

    public static final String SERVER_TIMING_HEADER_NAME = "Server-Timing";

    protected abstract String serverTimingKey();

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        var timingStart = (Instant) requestContext.getProperty(PROPERTY_SERVER_TIMING_START);
        if (timingStart == null) {
            return;
        }
        var duration = timingStart.until(Instant.now(), ChronoUnit.MILLIS);

        var responseHeaders = responseContext.getHeaders();
        var newTimingValue = serverTimingKey() + duration;

        // first ServerTiming-Header? add it, done.
        if (!responseHeaders.containsKey(SERVER_TIMING_HEADER_NAME)) {
            responseHeaders.putSingle(SERVER_TIMING_HEADER_NAME, newTimingValue);
            return;
        }
        // otherwise merge the current one with previously added ones
        var oldValues = responseContext.getStringHeaders().get(SERVER_TIMING_HEADER_NAME);
        var mergedServerTimings = mergeTimings(oldValues, newTimingValue);
        responseHeaders.putSingle(SERVER_TIMING_HEADER_NAME, mergedServerTimings);
    }

    /**
     * Merge Server-Timing headers so eventually a single header is returned. Metric keys which are
     * present multiple times are summed up into a single one (so {@code "t;dur=5, t;dur=10"}
     * becomes {@code "t;dur=15"}). The description part ({@code "desc="}) is removed.
     *
     * @param oldValues The currently present set of values for the {@code Server-Timing} header
     * @param newValue The new value which is about to be added to the {@code Server-Timing} header
     * @return a single String value for the {@code Server-Timing} header.
     */
    private static String mergeTimings(Collection<String> oldValues, String newValue) {
        // @formatter:off
        var timingMap = Stream.concat(oldValues.stream(), Stream.of(newValue))
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(AbstractServerTimingResponseFilter::removeDescription)
                .map(metric -> metric.contains(";dur=") ? metric : metric + ";dur=0")
                .map(String::trim)
                .map(metric -> metric.split(";", 2))
                .collect(Collectors.toUnmodifiableMap(
                        metricPart -> metricPart[0], // key
                        metricPart -> metricPart[1], // value
                        AbstractServerTimingResponseFilter::sumUpDurations)); // on key collisions
        // @formatter:on

        return timingMap.entrySet()
                .stream()
                .map(entry -> String.format("%s;%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String sumUpDurations(String metric1, String metric2) {
        var durationKey = "dur=";
        var metricValue1 = metric1.replace(durationKey, "");
        var metricValue2 = metric2.replace(durationKey, "");
        int metricDuration1;
        int metricDuration2;
        try {
            metricDuration1 = (int) Float.valueOf(metricValue1).floatValue();
            metricDuration2 = (int) Float.valueOf(metricValue2).floatValue();
        } catch (NumberFormatException e) {
            LOG.warnf("sumUpDurations failed: %s", e.getMessage());
            return durationKey + "0";
        }
        return durationKey + (metricDuration1 + metricDuration2);
    }

    private static String removeDescription(String serverTimingMetric) {
        // @formatter:off
        return serverTimingMetric
                .replaceAll("desc=\".*?\";", "") // quoted desc
                .replaceAll(";desc=\".*\"", "")  // quoted desc (last elem)
                .replaceAll("desc=.*?;", "")     // unquoted desc
                .replaceAll(";desc=.*", "");     // unquoted desc (last elem)
        // @formatter:on
    }
}
