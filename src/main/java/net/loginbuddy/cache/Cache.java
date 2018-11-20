package net.loginbuddy.cache;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    private Map<String, Object> cache;

    public Cache() {
        cache = new HashMap<>();
    }

    public void flush() {
        cache = new HashMap<>();
    }

    public boolean put(String key, Object value) {
        return cache.put(key,value) == null;
    }

    public boolean remove(String key) {
        return cache.remove(key) == null;
    }

    public Object get(String key)
    {
        return cache.get(key);
    }

    public Map<String, Object> getCache() {
        return cache;
    }

    public void setCache(Map<String, Object> cache) {
        this.cache = cache;
    }
}