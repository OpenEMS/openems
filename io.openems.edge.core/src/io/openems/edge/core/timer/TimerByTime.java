package io.openems.edge.core.timer;

import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.ValueInitializedWrapper;

/**
 * This Timer is one of the child Implementations of the {@link AbstractTimer}
 * and the {@link Timer}. It gets the {@link ValueInitializedWrapper} and checks
 * if the the current Time is after the initTime+MaxTimeInSeconds. Remember on
 * init -> Timer will be initialized and sets the Time. If you wish to Reset:
 * {@link Timer#reset(String id, String identifier)} (this will do:
 * {@link ValueInitializedWrapper#setInitialized(boolean)} (false)}
 */
@Designate(ocd = TimerByTimeConfig.class, factory = true)
@Component(//
		name = "Timer.ByTime", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class TimerByTime extends AbstractTimer implements OpenemsComponent {

	public TimerByTime() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, TimerByCountingConfig config) {
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
			return Instant.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
		} else {
			wrapper.setInitialized(true);
			wrapper.getInitialDateTime().set(Instant.now());
		}
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
