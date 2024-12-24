package io.openems.edge.energy.api.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.energy.api.EnergySchedulable;

public abstract class AbstractDummyEnergySchedulable<SELF extends AbstractDummyEnergySchedulable<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements EnergySchedulable, OpenemsComponent {

	protected AbstractDummyEnergySchedulable(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

}
