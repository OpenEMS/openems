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
	 * @param meter the {@link AsymmetricMeter}
	 * @param phase the {@link SinglePhase}
	 */
	public static void initializeCopyPhaseChannel(AsymmetricMeter meter, SinglePhase phase) {
		switch (phase) {
		case L1:
			meter.getActivePowerL1Channel().onSetNextValue(value -> {
				meter._setActivePower(value.get());
			});
			break;
		case L2:
			meter.getActivePowerL2Channel().onSetNextValue(value -> {
				meter._setActivePower(value.get());
			});
			break;
		case L3:
			meter.getActivePowerL3Channel().onSetNextValue(value -> {
				meter._setActivePower(value.get());
			});
			break;
		}
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SinglePhaseMeter.class, accessMode, 100) //
				.build();
	}
}
