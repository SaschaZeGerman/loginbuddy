package net.loginbuddy.common.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalStorage implements StorageProvider {

    private String storageName;
    private ConcurrentMap<String, Object> loginbuddyStorage;

    public LocalStorage() {
        this("loginbuddyStorage");
    }

    public LocalStorage(String storageName) {
        this.storageName = storageName;
        this.loginbuddyStorage = new ConcurrentHashMap<>();
    }

    public String getStorageName() {
        return storageName;
    }

    public void setLoginbuddyStorage(ConcurrentMap<String, Object> loginbuddyStorage) {
        this.loginbuddyStorage = loginbuddyStorage;
    }

    @Override
    public Object put(String key, Object value) {
        return loginbuddyStorage.put(key, value);
    }

    @Override
    public Object get(String key) {
        return loginbuddyStorage.get(key);
    }
}