package io.openems.edge.timer.api;

import org.joda.time.DateTime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The ValueInitializedWrapper is a Class that stores the important data the {@link Timer} needs to work.
 * It stores the maxWaitTime (e.g. maxCycles).
 * If the Identifier is initialized or not.
 * The Counter for MaxCycles and the initial DateTime.
 */
class ValueInitializedWrapper {

    private int maxValue;
    private boolean initialized;
    //only needed by CycleTimer
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicReference<DateTime> initialDateTime = new AtomicReference<>();

    public ValueInitializedWrapper(int maxValue, boolean initialized) {
        this.maxValue = maxValue;
        this.initialized = initialized;
        this.initialDateTime.set(new DateTime());
    }

    public ValueInitializedWrapper(int maxValue) {
        this(maxValue, false);
    }


    int getMaxValue() {
        return this.maxValue;
    }

    void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    boolean isInitialized() {
        return this.initialized;
    }

    void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    AtomicInteger getCounter() {
        return this.counter;
    }

    AtomicReference<DateTime> getInitialDateTime() {
        return this.initialDateTime;
    }

    void setInitialDateTime(DateTime time){
        this.initialDateTime.set(time);
    }
    void setCounter(int value){
        this.counter.set(value);
    }
}
