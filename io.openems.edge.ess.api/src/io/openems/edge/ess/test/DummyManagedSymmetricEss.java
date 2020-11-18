package io.openems.edge.ess.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated {@link ManagedSymmetricEss} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricEss extends AbstractDummyManagedSymmetricEss<DummyManagedSymmetricEss>
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	protected DummyManagedSymmetricEss(String id, Power power,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, power, firstInitialChannelIds, furtherInitialChannelIds);
	}

	public DummyManagedSymmetricEss(String id, Power power) {
		this(id, power, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
	}

	public DummyManagedSymmetricEss(String id) {
		this(id, new DummyPower(MAX_APPARENT_POWER));
	}

	@Override
	protected DummyManagedSymmetricEss self() {
		return this;
	}
}
