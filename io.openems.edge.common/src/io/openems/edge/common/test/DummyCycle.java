package io.openems.edge.common.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;

/**
 * Simulates a Cycle for the OpenEMS Component test framework.
 */
public class DummyCycle extends AbstractOpenemsComponent implements Cycle {

	private final int cycleTime;

	public DummyCycle(int cycleTime) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Cycle.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, "_cycle", "", true);
		this.cycleTime = cycleTime;
	}

	@Override
	public int getCycleTime() {
		return this.cycleTime;
	}

}