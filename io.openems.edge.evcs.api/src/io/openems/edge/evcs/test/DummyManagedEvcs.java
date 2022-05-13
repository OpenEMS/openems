package io.openems.edge.evcs.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;

public class DummyManagedEvcs extends AbstractOpenemsComponent implements Evcs, ManagedEvcs, OpenemsComponent {

	private final EvcsPower evcsPower;

	/**
	 * Constructor.
	 *
	 * @param id id
	 */
	public DummyManagedEvcs(String id, EvcsPower evcsPower) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		this.evcsPower = evcsPower;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
