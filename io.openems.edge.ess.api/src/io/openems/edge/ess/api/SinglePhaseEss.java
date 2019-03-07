package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.doc.Doc;

@ProviderType
public interface SinglePhaseEss extends AsymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
	 * Gets the Phase this ESS is connected to.
	 * 
	 * @return
	 */
	public SinglePhase getPhase();

	/**
	 * Initializes Channel listeners. Copies the Active-Power Phase-Channel value to
	 * Active-Power Channel.
	 * 
	 * @param ess
	 * @param phase
	 */
	public static void initializeCopyPhaseChannel(AsymmetricEss ess, SinglePhase phase) {
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
		}
	}
}
