package io.openems.edge.common.timer;

/**
 * This is the Interface for a child implementation of the TimerByCycles.
 * It is an interface to mark the TimerByCycles as well as storing the singleton service pid and component id.
 */
public interface TimerByCycles extends Timer {

    public final static String SINGLETON_SERVICE_PID = "Timer.ByCycles";
    public final static String SINGLETON_COMPONENT_ID = "_TimerByCycles";
}
