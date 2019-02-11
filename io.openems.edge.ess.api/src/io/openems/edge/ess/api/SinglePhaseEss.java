package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;

@ProviderType
public interface SinglePhaseEss extends AsymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * The Phase of this ESS
		 * 
		 * <ul>
		 * <li>Interface: Ess Single-Phase
		 * <li>Type: Integer
		 * <li>Range: 1, 2 or 3
		 * </ul>
		 */
		PHASE(new Doc() //
				.options(Phase.values()));

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
	 * Gets the Active Power on L1 in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Phase> getPhase() {
		return this.channel(ChannelId.PHASE);
	}

	/**
	 * Initializes Channel listeners. Copies the Active-Power Phase-Channel value to
	 * Active-Power Channel.
	 * 
	 * @param ess
	 * @param phase
	 */
	public static void initializeCopyPhaseChannel(AsymmetricEss ess, Phase phase) {
		switch (phase) {
		case L1:
			ess.getActivePowerL1().onSetNextValue(value -> {
				ess.getActivePower().setNextValue(value);
			});
			break;
		case L2:
			ess.getActivePowerL2().onSetNextValue(value -> {
				ess.getActivePower().setNextValue(value);
			});
			break;
		case L3:
			ess.getActivePowerL3().onSetNextValue(value -> {
				ess.getActivePower().setNextValue(value);
			});
			break;
		case UNDEFINED:
			break;
		}
	}
}
