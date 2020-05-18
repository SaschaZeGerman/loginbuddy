package net.loginbuddy.service.config;

import org.apache.http.MethodNotSupportedException;

public interface Loader {
    void load() throws Exception;
    void reload() throws Exception;
    <T> T save(T configuration) throws Exception;
    <T> T update(T configuration) throws Exception;
}
