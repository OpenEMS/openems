package io.openems.edge.common.sum;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple, simulated Sum component that can be used together with the
 * OpenEMS Component test framework.
 */
public class DummySum extends AbstractOpenemsComponent implements Sum, OpenemsComponent {

	public DummySum() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Sum.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, Sum.SINGLETON_COMPONENT_ID, Sum.SINGLETON_SERVICE_PID, true);
	}

	@Override
	public void updateChannelsBeforeProcessImage() {
		// nothing here
	}

}
