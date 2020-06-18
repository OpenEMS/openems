package io.openems.edge.evcs.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;

public class DummyManagedEvcs extends AbstractOpenemsComponent implements Evcs, ManagedEvcs, OpenemsComponent {

	/**
	 * Constructor.
	 * 
	 * @param id id
	 */
	public DummyManagedEvcs(String id) {
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
