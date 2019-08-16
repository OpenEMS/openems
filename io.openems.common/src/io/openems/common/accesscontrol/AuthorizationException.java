package io.openems.common.accesscontrol;

import io.openems.common.exceptions.OpenemsException;

public class AuthorizationException extends OpenemsException {

    public AuthorizationException() {
        this("Authorization did not succeed");
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
