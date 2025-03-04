package io.openems.edge.thermometer.test;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

public abstract class AbstractDummyThermometer<SELF extends AbstractDummyThermometer<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements Thermometer {

	protected AbstractDummyThermometer(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Set {@link Thermometer.ChannelId#TEMPERATURE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withTemperature(int value) {
		withValue(this, Thermometer.ChannelId.TEMPERATURE, value);
		return this.self();
	}
}
