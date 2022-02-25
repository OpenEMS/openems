package io.openems.edge.timer.api;

import org.joda.time.DateTime;

/**
 * The Timer interface. This is normally used by TimerHandler, but other classes can use the Timer directly. (Even though it's not recommended)
 * The Timer is used to measure, if a specific time is up. ATM it can be a cycle counter or it could be a real Time.
 * Usually Components instantiate a {@link TimerHandler} and add it's config to the Timerhandler. The Timerhandler
 * therefore communicates between a class and the timer itself, providing the info a class needs to determine if a certain time is up or not.
 * Key is the Id and the identifier. A class can have multiple identifier, each mapped to certain intitial Timestamps/Cyclecounts.
 * Making it possible to support "n" identifier.
 */
public interface Timer {
    /**
     * Resets the Timer for the Component calling this method. Multiple Timer per config are possible.
     *
     * @param id         the openemsComponent id
     * @param identifier the identifier the component uses
     */
    void reset(String id, String identifier);

    /**
     * Check if the Time for this Component is up.
     *
     * @param id         the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    boolean checkIsTimeUp(String id, String identifier);

    /**
     * Removes the Component from the Timer.
     *
     * @param id of the Component you want to remove
     */
    void removeComponent(String id);

    /**
     * Adds an Identifier to the Timer. An Identifier is a Unique Id within a Component.
     * This is important due to the fact, that a component may need multiple Timer, determining different results.
     *
     * @param id         the ComponentId
     * @param identifier the identifier
     * @param maxValue   the maxValue (max CycleTime or maxTime to wait)
     */
    void addIdentifierToTimer(String id, String identifier, int maxValue);

    void setInitTime(String id, String identifierSwap, DateTime dateTime);

    void setInitTime(String id, String identifierSwap, Integer count);
}
