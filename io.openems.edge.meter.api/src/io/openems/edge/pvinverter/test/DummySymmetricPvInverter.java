package io.openems.edge.pvinverter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

/**
 * Provides a simple, simulated SymmetricPvInverter component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummySymmetricPvInverter extends AbstractOpenemsComponent implements ManagedSymmetricPvInverter {

	public DummySymmetricPvInverter(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

}
