package io.openems.common.exceptions;

public class HeatTooLowException extends OpenemsException {
    public HeatTooLowException(String message) {
        super(message);
    }

    public HeatTooLowException(String message, Throwable cause) {
        super(message, cause);
    }

    public HeatTooLowException(Throwable cause) {
        super(cause);
    }
}
