package io.openems.edge.battery.test;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Provides a simple, simulated {@link Battery} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyBattery extends AbstractDummyBattery<DummyBattery>
		implements Battery, OpenemsComponent, StartStoppable {

	public DummyBattery(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BatteryProtection.ChannelId.values());
	}

	@Override
	protected final DummyBattery self() {
		return this;
	}

}
