package pt.haslab.alloy4fun.services;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pt.haslab.alloy4fun.data.models.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class SessionService {

    private final Map<String, Session> localSessions = new HashMap<>();

    @ConfigProperty(name = "session.timeout", defaultValue = "600")
    Long timeoutSeconds;

    public void update(Session session) {
        session.registerAccess();
        localSessions.put(session.id, session);
    }

    public Session findById(String _id) {
        Session result = localSessions.get(_id);
        if (result != null)
            result.registerAccess();
        return result;
    }

    public boolean deleteById(String _id) {
        return localSessions.remove(_id) != null;
    }


    @Scheduled(every = "${session.timeout:600}s", delay = 1, delayUnit = TimeUnit.SECONDS)
    public void deleteByLastAccessOutdated() {
        Set<String> r = localSessions.values()
                .stream()
                .filter(s -> s.isIdleForXSec(timeoutSeconds))
                .map(x -> x.id)
                .collect(Collectors.toSet());
        r.forEach(localSessions::remove);
    }

}
