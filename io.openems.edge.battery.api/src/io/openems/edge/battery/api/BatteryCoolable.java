package io.openems.edge.battery.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

public interface BatteryCoolable extends Battery, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Cooling valve state.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: {@link OpenemsType#Boolean}
		 * </ul>
		 */
		COOLING_VALVE_STATE(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //

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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(BatteryCoolable.class, accessMode, 10) //
				.channel(0, ChannelId.COOLING_VALVE_STATE, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getCoolingValveStateChannel() {
		return this.channel(ChannelId.COOLING_VALVE_STATE);
	}

	/**
	 * Gets the {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCoolingValveState() {
		return this.getCoolingValveStateChannel().value();
	}
}
