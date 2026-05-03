package com.payprosys.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Demo: in-memory session store. Token -> SessionInfo. */
@Component
public class SessionStore {

    private final Map<String, SessionInfo> store = new ConcurrentHashMap<>();

    public void put(String token, SessionInfo info) {
        store.put(token, info);
    }

    public Optional<SessionInfo> get(String token) {
        return Optional.ofNullable(store.get(token));
    }

    public void remove(String token) {
        store.remove(token);
    }
}
