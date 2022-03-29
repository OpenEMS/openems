package io.openems.edge.common.timer;

/**
 * This is the Interface for a child implementation of the TimerByTime.
 * It is an interface to mark the TimerByTime as well as storing the singleton service pid and component id.
 */
public interface TimerByTime extends Timer {

    public final static String SINGLETON_SERVICE_PID = "Timer.ByTime";
    public final static String SINGLETON_COMPONENT_ID = "_TimerByTime";
}
