package io.openems.edge.pvinverter.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

/**
 * Provides a simple, simulated SymmetricPvInverter component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummySymmetricPvInverter extends AbstractOpenemsComponent implements SymmetricPvInverter {

	public DummySymmetricPvInverter(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				SymmetricPvInverter.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, true);
	}

	@Override
	public void setActivePowerLimit(int activePowerWatt) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}

}
