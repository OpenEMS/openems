package io.openems.edge.ess.test;

import java.util.Arrays;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Provides a simple, simulated {@link MetaEss} (i.e. EssCluster) component that
 * can be used together with the OpenEMS Component test framework.
 */
public class DummyMetaEss extends AbstractDummyManagedSymmetricEss<DummyMetaEss>
		implements ManagedSymmetricEss, SymmetricEss, StartStoppable, OpenemsComponent, MetaEss {

	private final String[] essIds;

	public DummyMetaEss(String id, SymmetricEss... esss) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values());

		// Add all ManagedSymmetricEss devices to this.essIds
		this.essIds = Arrays.stream(esss).map(SymmetricEss::id).toArray(String[]::new);
	}

	@Override
	protected DummyMetaEss self() {
		return this;
	}

	@Override
	public String[] getEssIds() {
		return this.essIds;
	}

}