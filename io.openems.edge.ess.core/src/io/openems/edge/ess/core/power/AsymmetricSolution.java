package io.openems.edge.ess.core.power;

import io.openems.edge.ess.api.ManagedAsymmetricEss;

public class AsymmetricSolution extends SymmetricSolution {

	private final int activePowerL1;
	private final int reactivePowerL1;
	private final int activePowerL2;
	private final int reactivePowerL2;
	private final int activePowerL3;
	private final int reactivePowerL3;

	public AsymmetricSolution(ManagedAsymmetricEss ess, int activePowerL1, int reactivePowerL1, int activePowerL2,
			int reactivePowerL2, int activePowerL3, int reactivePowerL3) {
		super(ess, //
				activePowerL1 + activePowerL2 + activePowerL3, //
				reactivePowerL1 + reactivePowerL2 + reactivePowerL3);
		this.activePowerL1 = activePowerL1;
		this.reactivePowerL1 = reactivePowerL1;
		this.activePowerL2 = activePowerL2;
		this.reactivePowerL2 = reactivePowerL2;
		this.activePowerL3 = activePowerL3;
		this.reactivePowerL3 = reactivePowerL3;
	}

	public int getActivePowerL1() {
		return activePowerL1;
	}

	public int getReactivePowerL1() {
		return reactivePowerL1;
	}

	public int getActivePowerL2() {
		return activePowerL2;
	}

	public int getReactivePowerL2() {
		return reactivePowerL2;
	}

	public int getActivePowerL3() {
		return activePowerL3;
	}

	public int getReactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public String toString() {
		return "AsymmetricSolution [ess=" + this.getEss().id() + ", L1 P=" + activePowerL1 + ", Q=" + reactivePowerL1 + ", L2 P="
				+ activePowerL2 + ", Q=" + reactivePowerL2 + ", L3 P=" + activePowerL3 + ", Q=" + reactivePowerL3 + "]";
	}
}
