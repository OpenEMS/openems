package io.openems.edge.battery.test;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple, simulated ManagedSymmetricEss component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyBattery extends AbstractOpenemsComponent implements Battery, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	public DummyBattery(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

}
