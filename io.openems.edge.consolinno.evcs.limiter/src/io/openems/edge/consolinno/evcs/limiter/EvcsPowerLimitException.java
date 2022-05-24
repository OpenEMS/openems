package io.openems.edge.consolinno.evcs.limiter;

public class EvcsPowerLimitException extends EvcsException{
    public EvcsPowerLimitException() {
        super("ERROR IN POWER LIMITATION PROCESS");
    }
}
