package io.openems.edge.thermometer.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

/**
 * Provides a simple, simulated {@link Thermometer} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyThermometer extends AbstractDummyThermometer<DummyThermometer>
		implements Thermometer, OpenemsComponent {

	public DummyThermometer(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Thermometer.ChannelId.values());
	}

	@Override
	protected final DummyThermometer self() {
		return this;
	}
}
