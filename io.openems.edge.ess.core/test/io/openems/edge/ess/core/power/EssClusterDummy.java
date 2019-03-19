package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;

public class EssClusterDummy extends DummyComponent<EssClusterDummy> implements ManagedAsymmetricEss, MetaEss {

	private final List<ManagedSymmetricEss> managedEsss = new ArrayList<>();

	public EssClusterDummy(String id, SymmetricEss... esss) {
		super(id);

		/*
		 * Add all ManagedSymmetricEss devices to this.managedEsss
		 */
		for (SymmetricEss ess : esss) {
			if (ess instanceof ManagedSymmetricEss) {
				this.managedEsss.add((ManagedSymmetricEss) ess);
			}
		}
	}

	@Override
	public List<ManagedSymmetricEss> getEsss() {
		return this.managedEsss;
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
