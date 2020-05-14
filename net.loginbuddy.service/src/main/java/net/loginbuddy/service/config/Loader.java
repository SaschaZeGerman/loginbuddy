package net.loginbuddy.service.config;

import org.apache.http.MethodNotSupportedException;

public interface Loader {
    void load();
    void reload();
    <T> T save(T configuration) throws MethodNotSupportedException;
    <T> T update(T configuration) throws MethodNotSupportedException;
}
