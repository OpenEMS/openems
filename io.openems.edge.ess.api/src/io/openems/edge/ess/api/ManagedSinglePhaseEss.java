package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;

/**
 * Represents a Single-Phase Energy Storage System.
 */
@ProviderType
public interface ManagedSinglePhaseEss extends ManagedSymmetricEss, SinglePhaseEss {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Default implementation of {@link ManagedAsymmetricEss#applyPower(int, int)}
	 * for {@link ManagedSinglePhaseEss}.
	 * 
	 * @param activePowerL1   the active power set-point for L1
	 * @param reactivePowerL1 the reactive power set-point for L1
	 * @param activePowerL2   the active power set-point for L2
	 * @param reactivePowerL2 the reactive power set-point for L2
	 * @param activePowerL3   the active power set-point for L3
	 * @param reactivePowerL3 the reactive power set-point for L3
	 * @throws OpenemsNamedException on error
	 */
	public default void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		switch (this.getPhase()) {
		case L1:
			this.applyPower(activePowerL1, reactivePowerL1);
			break;
		case L2:
			this.applyPower(activePowerL2, reactivePowerL2);
			break;
		case L3:
			this.applyPower(activePowerL3, reactivePowerL3);
			break;
		}
	}
}
