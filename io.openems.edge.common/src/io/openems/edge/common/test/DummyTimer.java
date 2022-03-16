package io.openems.edge.common.test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerType;
import io.openems.edge.common.timer.ValueInitializedWrapper;

/**
 * A DummyTimer, provides BaseFunctionality for Unittests, that are using a
 * Timer.
 */
public class DummyTimer extends AbstractOpenemsComponent implements OpenemsComponent, Timer {

	private final Map<String, ValueInitializedWrapper> identifierToValueWrapper = new HashMap<>();
	private final TimerType type;

	public DummyTimer(String id, TimerType type) {
		super(ChannelId.values());
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
		this.type = type;
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
		ValueInitializedWrapper wrapper;
		switch (this.type) {

		case COUNTING:
		case CYCLES:
			wrapper = this.getWrapper(identifier);
			if (wrapper.isInitialized()) {
				return wrapper.getCounter().getAndIncrement() >= wrapper.getMaxValue();
			}
			wrapper.setInitialized(true);
			wrapper.getCounter().set(1);
			return false;
		case TIME:
			wrapper = this.getWrapper(identifier);
			if (wrapper.isInitialized()) {
				return Instant.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
			}
			wrapper.setInitialized(true);
			wrapper.getInitialDateTime().set(Instant.now());
			return false;
		}
		return true;
	}

	@Override
	public void removeComponent(String id) {
		// Not needed here
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
		var wrapper = this.getWrapper(identifier);
		if (wrapper != null) {
			wrapper.setInitialized(false);
		}
	}

	ValueInitializedWrapper getWrapper(String identifier) {
		return this.identifierToValueWrapper.get(identifier);
	}

	/**
	 * Adds an Identifier to the Timer. An Identifier is a Unique Id within a
	 * Component. This is important due to the fact, that a component may need
	 * multiple Timer, determining different results.
	 *
	 * @param id         the ComponentId
	 * @param identifier the identifier
	 * @param maxValue   the maxValue (max CycleTime or maxTime to wait)
	 */

	@Override
	public void addIdentifierToTimer(String id, String identifier, int maxValue) {
		this.identifierToValueWrapper.put(identifier, new ValueInitializedWrapper(maxValue));
	}

	@Override
	public void setInitTime(String id, String identifierSwap, Instant time) {
		if (this.type.equals(TimerType.TIME)) {
			this.identifierToValueWrapper.get(identifierSwap).setInitialized(true);
			this.identifierToValueWrapper.get(identifierSwap).setInitialDateTime(time);
		}
	}

	@Override
	public void setInitTime(String id, String identifierSwap, Integer count) {
		if (!this.type.equals(TimerType.TIME)) {
			this.identifierToValueWrapper.get(identifierSwap).setInitialized(true);
			this.identifierToValueWrapper.get(identifierSwap).setCounter(count);
		}
	}
}
