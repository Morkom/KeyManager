package dev.morkom.keymanager.service;

import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrivateKeyCache {

    private final Map<String, PrivateKey> cache = new ConcurrentHashMap<>();

    public String put(PrivateKey privateKey) {
        String id = UUID.randomUUID().toString();
        cache.put(id, privateKey);
        return id;
    }

    public PrivateKey get(String id) {
        return cache.get(id);
    }

    public void remove(String id) {
        cache.remove(id);
    }
}
