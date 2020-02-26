package io.openems.edge.meter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Provides a simple, simulated SymmetricMeter component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyAsymmetricMeter extends AbstractOpenemsComponent implements AsymmetricMeter {

	public final static int MAX_APPARENT_POWER = 10000;

	public DummyAsymmetricMeter(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

}
