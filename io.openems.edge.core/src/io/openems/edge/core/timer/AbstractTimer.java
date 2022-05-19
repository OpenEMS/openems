package io.openems.edge.core.timer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The Abstract Timer. It provides basic methods that both Timer
 * {@link TimerByTimeImpl} and {@link TimerByCountingImpl} use.
 */
public abstract class AbstractTimer implements Timer {

    /**
     * This Map connects the ComponentId with it's identifier and a WrapperClass
     * containing the maxCycle/Time value as an Int. and the Boolean if the
     * identifier was initialized or not. On reset set the boolean to false.
     */
    protected final Map<String, Map<String, ValueInitializedWrapper>> componentToIdentifierValueAndInitializedMap = new HashMap<>();

    /**
     * Removes the Component from the Timer.
     *
     * @param id of the Component you want to remove
     */
    @Override
    public void removeComponent(String id) {
        this.componentToIdentifierValueAndInitializedMap.remove(id);
    }

    /**
     * Resets the Timer for the Component calling this method. Multiple Timer per
     * config are possible.
     *
     * @param id         the openemsComponent id
     * @param identifier the identifier the component uses
     */
    @Override
    public void reset(String id, String identifier) {
        var wrapper = this.getWrapper(id, identifier);
        if (wrapper != null) {
            wrapper.setInitialized(false);
        }
    }

    /**
     * Returns the Stored ValueInitializedWrapper determined by the component id and
     * their identifier. Usually used by inheriting Classes.
     *
     * @param id         the ComponentId.
     * @param identifier the identifier asked for.
     * @return the {@link ValueInitializedWrapper}
     */
    protected ValueInitializedWrapper getWrapper(String id, String identifier) {
        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)
                && this.componentToIdentifierValueAndInitializedMap.get(id).containsKey(identifier)) {
            return this.componentToIdentifierValueAndInitializedMap.get(id).get(identifier);
        }
        return null;
    }

    /**
     * Adds an Identifier to the Timer. An Identifier is a Unique Id within a
     * Component. This is important due to the fact, that a component may need
     * multiple Timer, determining different results.
     *
     * @param id         the ComponentId the id of the component.
     * @param identifier one of the identifier the component has
     * @param maxValue   the maxValue (max CycleTime or maxTime to wait).
     */
    @Override
    public void addIdentifierToTimer(String id, String identifier, int maxValue) {
        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)) {
            if (this.componentToIdentifierValueAndInitializedMap.get(id).containsKey(identifier)) {
                this.getWrapper(id, identifier).setMaxValue(maxValue);
            } else {
                this.componentToIdentifierValueAndInitializedMap.get(id).put(identifier,
                        new ValueInitializedWrapper(maxValue));
            }
        } else {
            Map<String, ValueInitializedWrapper> identifierToValueMap = new HashMap<>();
            identifierToValueMap.put(identifier, new ValueInitializedWrapper(maxValue));
            this.componentToIdentifierValueAndInitializedMap.put(id, identifierToValueMap);
        }
    }

    /**
     * Overrides the Initial Time. Use with caution.
     *
     * @param id             the OpenemsComponent Id.
     * @param identifierSwap one of the identifier of the component.
     * @param time           the new initial Time.
     */
    @Override
    public void setInitTime(String id, String identifierSwap, Instant time) {
        this.getWrapper(id, identifierSwap).setInitialized(true);
        this.getWrapper(id, identifierSwap).setInitialDateTime(time);
    }

    /**
     * Overrides the Initial Time. Use with caution.
     *
     * @param id             the OpenemsComponent Id.
     * @param identifierSwap one of the identifier of the component.
     * @param count          new initial SetPoint of the counter.
     */
    @Override
    public void setInitTime(String id, String identifierSwap, Integer count) {
        this.getWrapper(id, identifierSwap).setInitialized(true);
        this.getWrapper(id, identifierSwap).setCounter(count);
    }
}
