package io.openems.common.exceptions;

public class NoHeatNeededException extends OpenemsException {

    public NoHeatNeededException(String message) {
        super(message);
    }

    public NoHeatNeededException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoHeatNeededException(Throwable cause) {
        super(cause);
    }
}
