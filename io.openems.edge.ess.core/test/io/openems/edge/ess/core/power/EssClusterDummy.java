package io.openems.edge.ess.core.power;

import java.util.Arrays;

import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;

public class EssClusterDummy extends DummyComponent<EssClusterDummy>
		implements ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, MetaEss {

	private final String[] essIds;

	public EssClusterDummy(String id, SymmetricEss... esss) {
		super(id);

		/*
		 * Add all ManagedSymmetricEss devices to this.managedEsss
		 */
		this.essIds = Arrays.stream(esss).map(e -> e.id()).toArray(String[]::new);
	}

	@Override
	public String[] getEssIds() {
		return this.essIds;
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		throw new IllegalArgumentException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	protected EssClusterDummy self() {
		return this;
	}
}
