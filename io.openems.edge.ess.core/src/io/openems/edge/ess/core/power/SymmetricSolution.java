package io.openems.edge.ess.core.power;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class SymmetricSolution {

	private final ManagedSymmetricEss ess;
	private final int activePower;
	private final int reactivePower;

	public SymmetricSolution(ManagedSymmetricEss ess, int activePower, int reactivePower) {
		this.ess = ess;
		this.activePower = activePower;
		this.reactivePower = reactivePower;
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}

	public int getActivePower() {
		return activePower;
	}

	public int getReactivePower() {
		return reactivePower;
	}

	@Override
	public String toString() {
		return "SymmetricSolution [ess=" + ess.id() + ", P=" + activePower + ", Q=" + reactivePower + "]";
	}
}
