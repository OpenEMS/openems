package io.openems.edge.core.timer;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerByTime;
import io.openems.edge.common.timer.ValueInitializedWrapper;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.time.Instant;

/**
 * This Timer is one of the child Implementations of the {@link AbstractTimer}
 * and the {@link Timer}. It gets the {@link ValueInitializedWrapper} and checks
 * if the current Time is after the initTime+MaxTimeInSeconds. Remember on
 * init -> Timer will be initialized and sets the Time. If you wish to Reset:
 * {@link Timer#reset(String id, String identifier)} (this will do:
 * {@link ValueInitializedWrapper#setInitialized(boolean)} (false)}
 */
@Designate(ocd = TimerByTimeConfig.class, factory = false)
@Component(//
        name = TimerByTime.SINGLETON_SERVICE_PID, //
        immediate = false, //
        property = { //
                "enabled=true" //
        })
public class TimerByTimeImpl extends AbstractTimer implements TimerByTime {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    protected ComponentManager componentManager;




    /**
     * Check if the Time for this Component is up.
     *
     * @param id         the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        var wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return Instant.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
        }
        wrapper.setInitialized(true);
        wrapper.getInitialDateTime().set(Instant.now());
        return false;
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
        super.setInitTime(id, identifierSwap, time);
    }

    @Override
    public void setInitTime(String id, String identifierSwap, Integer count) {
        // not supported here
    }
}
