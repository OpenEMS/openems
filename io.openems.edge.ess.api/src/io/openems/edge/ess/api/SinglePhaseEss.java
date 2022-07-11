package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;

@ProviderType
public interface SinglePhaseEss extends AsymmetricEss {

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
	 * @return the {@link SinglePhase}
	 */
	public SinglePhase getPhase();

	/**
	 * Initializes Channel listeners. Copies the Active-Power Phase-Channel value to
	 * Active-Power Channel.
	 *
	 * @param ess   the {@link AsymmetricEss}
	 * @param phase the {@link SinglePhase}
	 */
	public static void initializeCopyPhaseChannel(AsymmetricEss ess, SinglePhase phase) {
		switch (phase) {
		case L1:
			ess.getActivePowerL1Channel().onSetNextValue(value -> {
				ess._setActivePower(value.get());
			});
			break;
		case L2:
			ess.getActivePowerL2Channel().onSetNextValue(value -> {
				ess._setActivePower(value.get());
			});
			break;
		case L3:
			ess.getActivePowerL3Channel().onSetNextValue(value -> {
				ess._setActivePower(value.get());
			});
			break;
		}
	}
}
