package io.openems.edge.energy.api.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.energy.api.EnergySchedulable;

public abstract class AbstractDummyEnergySchedulable<SELF extends AbstractDummyEnergySchedulable<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements EnergySchedulable, OpenemsComponent {

	protected AbstractDummyEnergySchedulable(String factoryPid, String componentId,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(componentId, componentId, //
				new DummyComponentContext() //
						.addProperty("service.factoryPid", factoryPid), //
				firstInitialChannelIds, furtherInitialChannelIds);
	}
}
