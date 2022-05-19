package io.openems.edge.core.timer;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerByCounting;
import io.openems.edge.common.timer.ValueInitializedWrapper;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
 * Cycle to count only once each cycle (Therefore TimerByCounting) However, you
 * may call this timer more Frequently if you want. true, when X amount of calls
 * are done.
 */
@Designate(ocd = TimerByCountingConfig.class, factory = false)
@Component(//
        name = TimerByCounting.SINGLETON_SERVICE_PID,//
        immediate = false, //
        property = { //
                "enabled=true" //
        })
public class TimerByCountingImpl extends AbstractTimer implements TimerByCounting {


    @Reference
    ConfigurationAdmin cm;

    @Reference
    protected ComponentManager componentManager;

    /**
     * Check if the Time for this Component is up. Get the current Counter of the
     * {@link ValueInitializedWrapper#getCounter()} increment, and check if the
     * maxValue is reached. Note: After Init the return value will always be false.
     *
     * @param id         the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        var wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return wrapper.getCounter().getAndIncrement() >= wrapper.getMaxValue();
        }
        wrapper.setInitialized(true);
        wrapper.getCounter().set(1);
        return false;
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
        // Not supported here
    }
}
