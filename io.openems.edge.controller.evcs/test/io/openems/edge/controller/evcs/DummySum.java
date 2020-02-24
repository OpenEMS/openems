package io.openems.edge.controller.evcs;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;

public class DummySum extends AbstractOpenemsComponent implements OpenemsComponent, Sum {

	public DummySum() {
		super(OpenemsComponent.ChannelId.values(), //
				Sum.ChannelId.values());
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, "_sum", "", true);
	}
}
