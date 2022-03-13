package io.openems.edge.meter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;

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

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SinglePhaseMeter.class, accessMode, 100) //
				.build();
	}
}
