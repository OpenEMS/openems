package io.openems.common.accesscontrol;

import io.openems.common.exceptions.OpenemsException;

public class AuthenticationException extends OpenemsException {

    public AuthenticationException() {
        this("Authentication did not succeed");
    }

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
