package io.openems.edge.meter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;

@ProviderType
public interface SinglePhaseMeter extends ElectricityMeter {

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
	 * Gets the Phase this Meter is connected to.
	 *
	 * @return the Phase
	 */
	public SinglePhase getPhase();

	/**
	 * Initializes Channel listeners for a Symmetric {@link ElectricityMeter}.
	 * 
	 * <p>
	 * Sets the correct value for {@link ChannelId#ACTIVE_POWER_L1},
	 * {@link ChannelId#ACTIVE_POWER_L2} or {@link ChannelId#ACTIVE_POWER_L3} from
	 * {@link ChannelId#ACTIVE_POWER} by evaluating the configured
	 * {@link SinglePhase}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateSinglePhaseFromActivePower(SinglePhaseMeter meter) {
		meter.getActivePowerChannel().onSetNextValue(value -> {
			var phase = meter.getPhase();
			meter.getActivePowerL1Channel().setNextValue(phase == SinglePhase.L1 ? value : null);
			meter.getActivePowerL2Channel().setNextValue(phase == SinglePhase.L2 ? value : null);
			meter.getActivePowerL2Channel().setNextValue(phase == SinglePhase.L3 ? value : null);
		});
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
