package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

public enum EssType {

	META, SYMMETRIC, ASYMMETRIC;

	public static EssType getEssType(ManagedSymmetricEss ess) {
		if (ess instanceof MetaEss) {
			return META;
		} else if (ess instanceof ManagedAsymmetricEss) {
			return EssType.ASYMMETRIC;
		} else {
			return EssType.SYMMETRIC;
		}
	}
}
