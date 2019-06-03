package io.openems.common.access_control;

import io.openems.common.exceptions.OpenemsException;

public class ConfigurationException extends OpenemsException {

    public ConfigurationException() {
        this("Configuration is not valid");
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
