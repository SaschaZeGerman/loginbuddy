package net.loginbuddy.config.loginbuddy.exception;

public class RequiredConfigurationException extends RuntimeException {
    public RequiredConfigurationException(String message) {
        super(message);
    }
}