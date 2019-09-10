package io.openems.edge.evcs.keba.kecontact.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Evcs;

public class DummyKebaKeContact extends AbstractOpenemsComponent
		implements Evcs, ManagedEvcs, OpenemsComponent{

	public DummyKebaKeContact(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}
}

