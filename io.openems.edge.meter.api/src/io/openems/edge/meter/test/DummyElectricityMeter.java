package io.openems.edge.meter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

/**
 * Provides a simple, simulated ElectricityMeter component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyElectricityMeter extends AbstractOpenemsComponent implements ElectricityMeter {

	public static final int MAX_APPARENT_POWER = 10000;

	public DummyElectricityMeter(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
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
