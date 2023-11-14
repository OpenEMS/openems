package io.openems.edge.ess.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Provides a simple, simulated {@link ManagedSymmetricEss} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricEss extends AbstractDummyManagedSymmetricEss<DummyManagedSymmetricEss>
		implements ManagedSymmetricEss, SymmetricEss, StartStoppable, OpenemsComponent {

	public DummyManagedSymmetricEss(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values());
	}

	@Override
	protected final DummyManagedSymmetricEss self() {
		return this;
	}
}
