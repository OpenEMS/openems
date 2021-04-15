package io.openems.common.exceptions;

public class ValveDefectException extends OpenemsException {

    public ValveDefectException(String message) {
        super(message);
    }

    public ValveDefectException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValveDefectException(Throwable cause) {
        super(cause);
    }
}
