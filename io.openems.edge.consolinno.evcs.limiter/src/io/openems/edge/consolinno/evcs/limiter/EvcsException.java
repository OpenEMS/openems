package io.openems.edge.consolinno.evcs.limiter;

public class EvcsException extends Throwable {
    public EvcsException(String message) {
        super(message);
    }
    public EvcsException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvcsException(Throwable cause) {
        super(cause);
    }

}
