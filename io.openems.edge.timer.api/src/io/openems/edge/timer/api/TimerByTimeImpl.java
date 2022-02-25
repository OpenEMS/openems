package io.openems.edge.timer.api;


import io.openems.edge.common.component.OpenemsComponent;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This Timer is one of the child Implementations of the {@link AbstractTimer} and the {@link Timer}.
 * It gets the {@link ValueInitializedWrapper} and checks if the the current Time is after the initTime+MaxTimeInSeconds.
 * Remember on init -> Timer will be initialized and sets the Time.
 * If you wish to Reset: {@link Timer#reset(String id, String identifier)}
 * (this will do: {@link ValueInitializedWrapper#setInitialized(boolean)} (false)}
 */
@Designate(ocd = TimerByTimeConfig.class, factory = true)
@Component(name = "Timer.TimerByTime",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class TimerByTimeImpl extends AbstractTimer implements OpenemsComponent {

    public TimerByTimeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, TimerByCountingConfig config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, TimerByTimeConfig config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    /**
     * Check if the Time for this Component is up.
     *
     * @param id         the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        ValueInitializedWrapper wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return DateTime.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
        } else {
            wrapper.setInitialized(true);
            wrapper.getInitialDateTime().set(new DateTime());
        }
        return false;
    }

    @Override
    public void setInitTime(String id, String identifierSwap, DateTime dateTime) {
       super.setInitTime(id, identifierSwap, dateTime);
    }

    @Override
    public void setInitTime(String id, String identifierSwap, Integer count) {

    }
}
