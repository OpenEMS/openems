package io.openems.edge.meter.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Provides a simple, simulated {@link ElectricityMeter} component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyElectricityMeter extends AbstractDummyElectricityMeter<DummyElectricityMeter>
		implements ElectricityMeter {

	public DummyElectricityMeter(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Override
	protected DummyElectricityMeter self() {
		return this;
	}

}
