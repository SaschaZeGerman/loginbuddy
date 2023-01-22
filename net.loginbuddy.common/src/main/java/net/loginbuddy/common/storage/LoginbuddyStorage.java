package net.loginbuddy.common.storage;

import java.util.logging.Logger;

public enum LoginbuddyStorage implements Storage {

    STORAGE;

    private final Logger LOGGER = Logger.getLogger(LoginbuddyStorage.class.getName());

    private StorageProvider storageProvider;

    LoginbuddyStorage() {
        try {
            String storageType = System.getenv("STORAGE");
            if (storageType != null) {
                if (storageType.toUpperCase().startsWith("HAZELCAST")) {
                    try {
                        storageProvider = new RemoteStorage(storageType);
                        LOGGER.info("Connected to hazelcast server(s) as storage container");
                    } catch (Exception e) {
                        LOGGER.warning(String.format("Hazelcast cluster cannot be reached. No storage available. Error: %s", e.getMessage()));
                    }
                }
            }
            if (storageProvider == null) {
                storageProvider = new LocalStorage();
                LOGGER.info("Connected to local storage, Loginbuddy cluster is not supported");
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("LoginbuddyStorage could not be loaded! Error: '%s'", e.getMessage()));
        }
    }

    @Override
    public Object put(String key, Object obj) {
        return storageProvider.put(key, obj);
    }

    @Override
    public Object putIfNull(String key, Object obj) {
        if(storageProvider.get(key) == null) {
            put(key, obj);
        }
        return null;
    }

    @Override
    public Object get(String key) {
        return storageProvider.get(key);
    }
}