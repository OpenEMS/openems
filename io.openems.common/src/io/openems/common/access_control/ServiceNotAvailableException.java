package io.openems.common.access_control;

import io.openems.common.exceptions.OpenemsException;

public class ServiceNotAvailableException extends OpenemsException {

    public ServiceNotAvailableException() {
        this("The service cannot get used since it is not available yet");
    }

    public ServiceNotAvailableException(String message) {
        super(message);
    }

    public ServiceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
