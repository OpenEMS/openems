package io.openems.edge.common.access_control;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

class DummyOpenEmsComponent extends AbstractOpenemsComponent {

	DummyOpenEmsComponent(String id) {
		super(OpenemsComponent.ChannelId.values());
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, true);
	}

}
