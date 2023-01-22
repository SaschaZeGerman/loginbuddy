package net.loginbuddy.common.storage;

public interface StorageProvider {

    Object put(String key, Object value);
    Object get(String key);

}
