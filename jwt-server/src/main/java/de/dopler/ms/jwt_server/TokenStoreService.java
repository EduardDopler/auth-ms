package de.dopler.ms.jwt_server;

import javax.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class TokenStoreService {

    private final Map<String, Map<String, Object>> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> expirations = new ConcurrentHashMap<>();

    public void put(String tokenId, Map<String, Object> claims, long expiration) {
        tokens.put(tokenId, claims);
        expirations.put(tokenId, expiration);
    }

    public Map<String, Object> pop(String tokenId) {
        expirations.remove(tokenId);
        return tokens.remove(tokenId);
    }

    public long cleanup() {
        long removed = 0;
        var now = Instant.now(Clock.systemDefaultZone()).getEpochSecond();

        var candidates = expirations.entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(now) < 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (String candidate : candidates) {
            tokens.remove(candidate);
            expirations.remove(candidate);
            removed++;
        }

        return removed;
    }
}
