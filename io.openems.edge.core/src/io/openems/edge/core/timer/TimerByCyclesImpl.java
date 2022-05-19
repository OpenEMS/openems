package io.openems.edge.core.timer;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerByCycles;
import io.openems.edge.common.timer.ValueInitializedWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.time.Instant;

/**
 * This Timer is one of the child Implementations of the {@link AbstractTimer}
 * and the {@link Timer}. It gets the {@link ValueInitializedWrapper} and checks
 * if the current Counter is above or equals to the maximum. Remember on
 * init -> the counter will initialized and set to 1. Call the
 * {@link Timer#reset(String id, String identifier)} method, if you wish to
 * reset (this will do: {@link ValueInitializedWrapper#setInitialized(boolean)}
 * (false)} Usually you call this Timer via the TimerHandler and only once per
 * Cycle However, you may call this timer more Frequently if you want. true,
 * when X amount of calls are done. Everytime a new OpenEMS Cycle is started,
 * this Timer Counts each {@link ValueInitializedWrapper} up by one, except if
 * it is not initialized. If the Wrapper is not initialized, wait for the first
 * {@link #checkIsTimeUp(String, String)} call and then start to count the
 * cycles.
 */
@Designate(ocd = TimerByCyclesConfig.class, factory = false)
@Component(//
        name = TimerByCycles.SINGLETON_SERVICE_PID, //
        immediate = false, //
        property = { //
                "enabled=true", //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
        })
public class TimerByCyclesImpl extends AbstractTimer implements EventHandler, TimerByCycles {


    @Reference
    protected ComponentManager componentManager;

    /**
     * Check if the Time for this Component is up. Get the current Counter of the
     * {@link ValueInitializedWrapper#getCounter()}, and check if the maxValue is
     * reached. *
     *
     * @param id         the OpenEmsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        var wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return wrapper.getCounter().get() >= wrapper.getMaxValue();
        }
        wrapper.setInitialized(true);
        wrapper.getCounter().set(1);
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.componentToIdentifierValueAndInitializedMap.size() > 0) {
                this.componentToIdentifierValueAndInitializedMap.forEach((component, internalMap) -> {
                    internalMap.forEach((entry, initializedWrapper) -> {
                        if (initializedWrapper.isInitialized()) {
                            initializedWrapper.getCounter().getAndIncrement();
                        }
                    });
                });
            }
        }
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
        super.setInitTime(id, identifierSwap, count);
    }

    @Override
    public void setInitTime(String id, String identifierSwap, Instant time) {
        // not supported here
    }
}
