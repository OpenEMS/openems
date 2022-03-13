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
