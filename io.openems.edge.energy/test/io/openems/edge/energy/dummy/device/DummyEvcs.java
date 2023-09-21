package io.openems.edge.energy.dummy.device;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.simulatable.Simulatable;
import io.openems.edge.energy.api.simulatable.Simulator;

public class DummyEvcs extends AbstractOpenemsComponent implements OpenemsComponent, Simulatable {

	protected DummyEvcs(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyEvcs(String id) {
		this(id, //
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Override
	public Simulator getSimulator() {
//		return new Simulator() {
//
//			@Override
//			public void simulate(Period period) {
//				// TODO Auto-generated method stub
//			}
//		};
		return null;
	}
}
