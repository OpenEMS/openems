package io.openems.edge.pvinverter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

/**
 * Provides a simple, simulated ManagedSymmetricPvInverter Component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricPvInverter extends AbstractOpenemsComponent
		implements ElectricityMeter, OpenemsComponent, ManagedSymmetricPvInverter {

	protected DummyManagedSymmetricPvInverter(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, null, true);
	}

	public DummyManagedSymmetricPvInverter(String id) {
		this(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}
}
