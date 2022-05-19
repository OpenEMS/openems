package io.openems.edge.common.timer;

/**
 * This is the Interface for a child implementation of the TimerByCounting.
 * It is an interface to mark the TimerByCounting as well as storing the singleton service pid and component id.
 */
public interface TimerByCounting extends Timer {
    public final static String SINGLETON_SERVICE_PID = "Timer.ByCounting";
    public final static String SINGLETON_COMPONENT_ID = "_TimerByCounting";
}
