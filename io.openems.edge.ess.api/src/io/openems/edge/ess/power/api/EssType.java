package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

public enum EssType {

	META, SYMMETRIC, ASYMMETRIC, SINGLE_PHASE;

	/**
	 * Gets the {@link EssType} of the given {@link ManagedSymmetricEss}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the {@link EssType}
	 */
	public static EssType getEssType(ManagedSymmetricEss ess) {
		if (ess instanceof MetaEss) {
			return META;
		}
		if (ess instanceof ManagedSinglePhaseEss) {
			return EssType.SINGLE_PHASE;
		} else if (ess instanceof ManagedAsymmetricEss) {
			return EssType.ASYMMETRIC;
		} else {
			return EssType.SYMMETRIC;
		}
	}
}
