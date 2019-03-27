package io.openems.edge.meter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;

@ProviderType
public interface SinglePhaseMeter extends AsymmetricMeter {

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
	 * Gets the Phase this ESS is connected to.
	 * 
	 * @return the Phase
	 */
	public SinglePhase getPhase();

	/**
	 * Initializes Channel listeners. Copies the Active-Power Phase-Channel value to
	 * Active-Power Channel.
	 * 
	 * @param meter the AsymmetricMeter
	 * @param phase the Phase
	 */
	public static void initializeCopyPhaseChannel(AsymmetricMeter ess, SinglePhase phase) {
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
