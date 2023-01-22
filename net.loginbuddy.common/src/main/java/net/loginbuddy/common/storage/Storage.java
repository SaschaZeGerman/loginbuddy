package net.loginbuddy.common.storage;

public interface Storage {

    Object put(String key, Object value);
    Object putIfNull(String key, Object value);
    Object get(String key);

}
