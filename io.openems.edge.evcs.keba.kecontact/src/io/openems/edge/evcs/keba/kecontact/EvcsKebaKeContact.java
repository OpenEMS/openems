package io.openems.edge.evcs.keba.kecontact;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

public interface EvcsKebaKeContact extends ManagedEvcs, Evcs, OpenemsComponent, EventHandler, ModbusSlave {

	public static final int UDP_PORT = 7090;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * Report 1
		 */
		PRODUCT(Doc.of(OpenemsType.STRING) //
				.text("Model name (variant)")), //
		SERIAL(Doc.of(OpenemsType.STRING) //
				.text("Serial number")), //
		FIRMWARE(Doc.of(OpenemsType.STRING) //
				.text("Firmware version")), //
		COM_MODULE(Doc.of(OpenemsType.STRING) //
				.text("Communication module is installed; KeContact P30 only")),
		DIP_SWITCH_1(Doc.of(OpenemsType.STRING) //
				.text("The first eight dip switch settings as binary")),
		DIP_SWITCH_2(Doc.of(OpenemsType.STRING) //
				.text("The second eight dip switch settings as binary")),
		DIP_SWITCH_MAX_HW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("The raw maximum limit configured by the dip switches")),
		PHASE_SWITCH_COOLDOWN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Time remaining for the phase switch cooldown")), //

		/*
		 * Report 2
		 */
		STATUS_KEBA(Doc.of(Status.values()) //
				.text("Current state of the charging station")),
		ERROR_1(Doc.of(OpenemsType.INTEGER) //
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		ERROR_2(Doc.of(OpenemsType.INTEGER) //
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		PLUG(Doc.of(Plug.values())), //
		ENABLE_SYS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enable state for charging (contains Enable input, RFID, UDP,..)")), //
		ENABLE_USER(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enable condition via UDP")), //
		MAX_CURR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value via Control pilot")), //
		MAX_CURR_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.text("Current preset value via Control pilot in 0,1% of the PWM value")), //
		CURR_USER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value of the user via UDP; Default = 63000mA")), //
		CURR_FAILSAFE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value for the Failsafe function")), //
		TIMEOUT_FAILSAFE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Communication timeout before triggering the Failsafe function")), //
		CURR_TIMER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Shows the current preset value of currtime")), //
		TIMEOUT_CT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Shows the remaining time until the current value is accepted")), //
		OUTPUT(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.text("State of the output X2")), //
		INPUT(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.text("State of the potential free Enable input X1. When using the input, "
						+ "please pay attention to the information in the installation manual.")), //
		X2_PHASE_SWITCH(Doc.of(OpenemsType.INTEGER) //
				.text("Used phases by external phase switch")), //
		X2_PHASE_SWITCH_SOURCE(Doc.of(OpenemsType.INTEGER) //
				.text("Specified communication channel of x2")), //
		/*
		 * Report 3
		 */
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.text("Voltage on L1")), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.text("Voltage on L2")), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.text("Voltage on L3")), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current on L1")), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current on L2")), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current on L3")), //
		ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT) //
				.text("Total real power")), //
		COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("Power factor")), //
		ENERGY_TOTAL(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.text("Total power consumption (persistent) without current loading session. "
						+ "Is summed up after each completed charging session")), //
		DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Dip-Switch 1.3. for communication must be on")), //
		DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("A static ip is configured. The Dip-Switch 2.6. must be on")), //
		DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("A dynamic ip is configured. Either the Dip-Switch 2.6. must be off or a static ip has to be configured")), //
		DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM(Doc.of(Level.INFO) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Master-Slave communication is configured. If this is a normal KEBA that should be not controlled by a KEBA x-series, Dip-Switch 2.5. should be off")), //
		DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION(Doc.of(Level.WARNING) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Installation mode is configured. If the installation has finished, Dip-Switch 2.8. should be off")), //
		PRODUCT_SERIES_IS_NOT_COMPATIBLE(Doc.of(Level.FAULT) //
				.text("Keba e- and b-series cannot be controlled because their software and hardware are not designed for it.")), //
		NO_ENERGY_METER_INSTALLED(Doc.of(Level.INFO) //
				.text("This keba cannot measure energy values, because there is no energy meter in it.")), //
		CHARGINGSTATION_STATE_ERROR(Doc.of(Level.WARNING) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode), //
				ManagedEvcs.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Gets the Channel for {@link ChannelId#X2_PHASE_SWITCH}.
	 *
	 * @return the Channel as an IntegerReadChannel
	 */
	default IntegerReadChannel getX2PhaseSwitchChannel() {
		return this.channel(ChannelId.X2_PHASE_SWITCH);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#X2_PHASE_SWITCH}
	 * Channel.
	 *
	 * @param value the next value
	 */
	default void _setX2PhaseSwitch(int value) {
		this.getX2PhaseSwitchChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#X2_PHASE_SWITCH_SOURCE}.
	 *
	 * @return the Channel as an IntegerReadChannel
	 */
	default IntegerReadChannel getX2PhaseSwitchSourceChannel() {
		return this.channel(ChannelId.X2_PHASE_SWITCH_SOURCE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#X2_PHASE_SWITCH_SOURCE} Channel.
	 *
	 * @param value the next value
	 */
	default void _setX2PhaseSwitchSource(int value) {
		this.getX2PhaseSwitchSourceChannel().setNextValue(value);
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(EvcsKebaKeContact.class, accessMode, 300) //
				.channel(0, EvcsKebaKeContact.ChannelId.PRODUCT, ModbusType.STRING16)
				.channel(16, EvcsKebaKeContact.ChannelId.SERIAL, ModbusType.STRING16)
				.channel(32, EvcsKebaKeContact.ChannelId.FIRMWARE, ModbusType.STRING16)
				.channel(48, EvcsKebaKeContact.ChannelId.COM_MODULE, ModbusType.STRING16)
				.channel(64, EvcsKebaKeContact.ChannelId.STATUS_KEBA, ModbusType.UINT16)
				.channel(65, EvcsKebaKeContact.ChannelId.ERROR_1, ModbusType.UINT16)
				.channel(66, EvcsKebaKeContact.ChannelId.ERROR_2, ModbusType.UINT16)
				.channel(67, EvcsKebaKeContact.ChannelId.PLUG, ModbusType.UINT16)
				.channel(68, EvcsKebaKeContact.ChannelId.ENABLE_SYS, ModbusType.UINT16)
				.channel(69, EvcsKebaKeContact.ChannelId.ENABLE_USER, ModbusType.UINT16)
				.channel(70, EvcsKebaKeContact.ChannelId.MAX_CURR_PERCENT, ModbusType.UINT16)
				.channel(71, EvcsKebaKeContact.ChannelId.CURR_USER, ModbusType.UINT16)
				.channel(72, EvcsKebaKeContact.ChannelId.CURR_FAILSAFE, ModbusType.UINT16)
				.channel(73, EvcsKebaKeContact.ChannelId.TIMEOUT_FAILSAFE, ModbusType.UINT16)
				.channel(74, EvcsKebaKeContact.ChannelId.CURR_TIMER, ModbusType.UINT16)
				.channel(75, EvcsKebaKeContact.ChannelId.TIMEOUT_CT, ModbusType.UINT16).uint16Reserved(76)
				.channel(77, EvcsKebaKeContact.ChannelId.OUTPUT, ModbusType.UINT16)
				.channel(78, EvcsKebaKeContact.ChannelId.INPUT, ModbusType.UINT16)

				// Report 3
				.channel(79, EvcsKebaKeContact.ChannelId.VOLTAGE_L1, ModbusType.UINT16)
				.channel(80, EvcsKebaKeContact.ChannelId.VOLTAGE_L2, ModbusType.UINT16)
				.channel(81, EvcsKebaKeContact.ChannelId.VOLTAGE_L3, ModbusType.UINT16)
				.channel(82, EvcsKebaKeContact.ChannelId.CURRENT_L1, ModbusType.UINT16)
				.channel(83, EvcsKebaKeContact.ChannelId.CURRENT_L2, ModbusType.UINT16)
				.channel(84, EvcsKebaKeContact.ChannelId.CURRENT_L3, ModbusType.UINT16)
				.channel(85, EvcsKebaKeContact.ChannelId.ACTUAL_POWER, ModbusType.UINT16)
				.channel(86, EvcsKebaKeContact.ChannelId.COS_PHI, ModbusType.UINT16).uint16Reserved(87)
				.channel(88, EvcsKebaKeContact.ChannelId.ENERGY_TOTAL, ModbusType.UINT16).build();
	}
}
