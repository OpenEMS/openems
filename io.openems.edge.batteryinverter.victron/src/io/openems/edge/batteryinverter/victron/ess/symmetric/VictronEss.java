package io.openems.edge.batteryinverter.victron.ess.symmetric;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;

import io.openems.common.channel.PersistencePriority;
//import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.ShortWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.battery.victron.VictronBattery;
import io.openems.edge.batteryinverter.victron.ro.VictronBatteryInverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.victron.enums.ActiveInputSource;
import io.openems.edge.victron.enums.Alarm;
import io.openems.edge.victron.enums.AllowDisallow;
import io.openems.edge.victron.enums.EnableDisable;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.SwitchPosition;
import io.openems.edge.victron.enums.VEBusBMSError;
import io.openems.edge.victron.enums.VEBusError;
import io.openems.edge.victron.enums.VEBusState;
import io.openems.edge.victron.enums.ErrorYesNo;
import io.openems.edge.victron.enums.ChargeStateEss;

public interface VictronEss extends OpenemsComponent, EventHandler, ModbusComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
		),

		SET_ACTIVE_POWER(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REACTIVE_POWER(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_REACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_REACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_REACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)),

		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		FREQUENCY_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_ONLY)),

		FREQUENCY_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_ONLY)),

		FREQUENCY_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_OUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_OUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_OUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_OUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_OUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_OUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		FREQUENCY_OUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_ONLY)),

		POWER_OUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		POWER_OUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		POWER_OUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY)),

		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)),

		CURRENT_INPUT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)),

		PHASE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)),

		ACTIVE_INPUT(Doc.of(ActiveInputSource.values()) // 0=AC Input, 1=AC Input 2
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_STATE(Doc.of(VEBusState.values()) // 0=Off;1=Low
													// Power;2=Fault;3=Bulk;4=Absorption;5=Float;6=Storage;7=Equalize;8=Passthru;9=Inverting;10=Power
													// assist;11=Power supply;244=Sustain;252=External control
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_ERROR(Doc.of(VEBusError.values()) // 0=No error;1=VE.Bus Error 1: Device is switched off because one of
													// the other phases in the system has switched off;2=VE.Bus Error 2:
													// New and old types MK2 are mixed in the system;3=VE.Bus Error 3:
													// Not all- or more than- the expected devices were found in the
				.accessMode(AccessMode.READ_ONLY)),

		SWITCH_POSITION(Doc.of(SwitchPosition.values()) // 1=Charger Only;2=Inverter Only;3=On;4=Off
				.accessMode(AccessMode.READ_WRITE)),

		TEMPERATURE_ALARM(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		LOW_BATTERY_ALARM(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		OVERLOAD_ALARM(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		ESS_POWER_SETPOINT_PHASE_1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		ESS_POWER_SETPOINT_PHASE_2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		ESS_POWER_SETPOINT_PHASE_3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		// The following 3 have an signed Integer 32!
		INT32_ESS_POWER_SETPOINT_PHASE_1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		INT32_ESS_POWER_SETPOINT_PHASE_2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		INT32_ESS_POWER_SETPOINT_PHASE_3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		ESS_DISABLE_CHARGE_FLAG(Doc.of(EnableDisable.values()) // 0=Charge allowed;1=Charge disabled / Attention -
																// Inverse logic
				.accessMode(AccessMode.READ_WRITE)),

		ESS_DISABLE_FEEDBACK_FLAG(Doc.of(EnableDisable.values()) // 0=Feed in allowed;1=Feed in disabled / Attention -
																	// Inverse logic
				.accessMode(AccessMode.READ_WRITE)),

		TEMPERATURE_SENSOR_ALARM(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_BMS_EXPECTED(Doc.of(ErrorYesNo.values()) // 0=0;1=Yes
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_BMS_ERROR(Doc.of(ErrorYesNo.values()) // 0=0;1=Yes
				.accessMode(AccessMode.READ_ONLY)),

		VOLTAGE_SENSOR_ALARM(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		RIPPLE_ALARM_L1(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		RIPPLE_ALARM_L2(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		RIPPLE_ALARM_L3(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		TEMPERATURE_ALARM_L1(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		TEMPERATURE_ALARM_L2(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		TEMPERATURE_ALARM_L3(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		LOW_BATTERY_ALARM_L1(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		LOW_BATTERY_ALARM_L2(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		LOW_BATTERY_ALARM_L3(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		OVERLOAD_ALARM_L1(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		OVERLOAD_ALARM_L2(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		OVERLOAD_ALARM_L3(Doc.of(Alarm.values()) // 0=Ok;1=Warning;2=Alarm
				.accessMode(AccessMode.READ_ONLY)),

		DISABLE_PV_INVERTER(Doc.of(EnableDisable.values()) // 0=PV enabled;1=PV disabled
				.accessMode(AccessMode.READ_WRITE)),

		VE_BUS_BMS_ALLOW_BATTERY_CHARGE(Doc.of(AllowDisallow.values()) // 0=Disallow 1= Allow
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_BMS_ALLOW_BATTERY_DISCHARGE(Doc.of(AllowDisallow.values()) // 0=Disallow 1= Allow
				.accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY)),

		VE_BUS_RESET(Doc.of(OpenemsType.INTEGER) // 1=VE.Bus reset
				.accessMode(AccessMode.READ_WRITE)),

		PHASE_ROTATION_WARNING(Doc.of(Alarm.values()) // 1=Warning
				.accessMode(AccessMode.READ_ONLY)),

		SOLAR_OFFSET_VOLTAGE(Doc.of(OpenemsType.INTEGER) // 0=OvervoltageFeedIn uses 1V offset; 1=OvervoltageFeedIn uses
															// 0.1V offset
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

		SUSTAIN_ACTIVE(Doc.of(ActiveInactive.values()).accessMode(AccessMode.READ_ONLY)),

		ENERGY_FROM_AC_IN_1_TO_AC_OUT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_IN_1_TO_BATTERY(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_IN_2_TO_AC_OUT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_IN_2_TO_BATTERY(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_OUT_TO_AC_IN_1(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_OUT_TO_AC_IN_2(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_BATTERY_TO_AC_IN_1(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_BATTERY_TO_AC_IN_2(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_BATTERY_TO_AC_OUT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		ENERGY_FROM_AC_OUT_TO_BATTERY(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

		LOW_CELL_VOLTAGE_IMMINENT(Doc.of(Alarm.values()) // 0=OK;1=Warning
				.accessMode(AccessMode.READ_ONLY)),

		CHARGE_STATE(Doc.of(ChargeStateEss.values()) // 0=Initialising;1=Bulk;2=Absorption;3=Float;4=Storage;5=Absorb
														// repeat;6=Forced absorb;7=Equalise;8=Bulk stopped;9=Unknown
				.accessMode(AccessMode.READ_ONLY)),

		PREFER_RENEWABLE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

		SELECT_REMOTE_GENERATOR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

		REMOTE_GENERATOR_SELECTED(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

		FEED_DC_OVERVOLTAGE_TO_GRID(Doc.of(OpenemsType.INTEGER) // 0=Feed in overvoltage;1=Do not feed in overvoltage
				.accessMode(AccessMode.READ_ONLY)),

		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L1(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L2(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L3(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
		AC_INPUT1_IGNORED(Doc.of(EnableDisable.values()) // 0=AC input not ignored;1=AC input ignored
				.accessMode(AccessMode.READ_ONLY)),
		AC_INPUT2_IGNORED(Doc.of(EnableDisable.values()) // 0=AC input not ignored;1=AC input ignored
				.accessMode(AccessMode.READ_ONLY)),
/*		
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
*/				
		
		/**
		 * current capacity of battery. Does not make use of emergency capacity
		 *
		 * <ul>
		 * <li>Interface: VictronESS
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 *
		 */
		USEABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) // 
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		
		/**
		 * current useable soc of ess.
		 * Values from controllers are (ctrlEmergencyCapacityReserves and ctrlLimitTotalDischarges) are substracted
		 *
		 * <ul>
		 * <li>Interface: VictronBattery
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 *
		 */
		USEABLE_SOC(Doc.of(OpenemsType.INTEGER) // 
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)), 		

		AC_POWER_SETPOINT_AS_FEED_IN_LIMIT(Doc.of(OpenemsType.INTEGER) // 0=AcPowerSetpoint interpreted normally;
																		// 1=AcPowerSetpoint is OvervoltageFeedIn limit
				.accessMode(AccessMode.READ_WRITE)),

		GRID_LOST_ALARM(Doc.of(Alarm.values()) // 0= OK, 2=Alarm
				.accessMode(AccessMode.READ_ONLY));

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
	 * Gets the Channel for Voltage on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL1Channel() {
		return this.channel(ChannelId.VOLTAGE_L1);
	}

	/**
	 * Gets the Voltage on L1 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL1() {
		return this.getVoltageL1Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL2Channel() {
		return this.channel(ChannelId.VOLTAGE_L2);
	}

	/**
	 * Gets the Voltage on L2 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL2() {
		return this.getVoltageL2Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL3Channel() {
		return this.channel(ChannelId.VOLTAGE_L3);
	}

	/**
	 * Gets the Voltage on L3 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL3() {
		return this.getVoltageL3Channel().value();
	}

	/**
	 * Gets the Channel for Current on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL1Channel() {
		return this.channel(ChannelId.CURRENT_L1);
	}

	/**
	 * Gets the Current on L1 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1() {
		return this.getCurrentL1Channel().value();
	}

	/**
	 * Gets the Channel for Current on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL2Channel() {
		return this.channel(ChannelId.CURRENT_L2);
	}

	/**
	 * Gets the Current on L2 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2() {
		return this.getCurrentL2Channel().value();
	}

	/**
	 * Gets the Channel for Current on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL3Channel() {
		return this.channel(ChannelId.CURRENT_L3);
	}

	/**
	 * Gets the Current on L3 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3() {
		return this.getCurrentL3Channel().value();
	}

	// Set Active Power
	public default void _setActivePower(Short value) throws OpenemsNamedException {
		this.getChargePowerChannel().setNextWriteValue(value);
	}

	public default Value<Short> getChargePower() {
		return this.getChargePowerChannel().value();
	}

	public default ShortWriteChannel getChargePowerChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER);
	}

	// SetPoint Channel L1
	public default void _setSymmetricEssActivePowerL1(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL1().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL1() {
		return this.getChargePowerChannelL1().value();
	}

	public default ShortWriteChannel getChargePowerChannelL1() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1);
	}

	// SetPoint Channel L2
	public default void _setSymmetricEssActivePowerL2(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL2().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL2() {
		return this.getChargePowerChannelL2().value();
	}

	public default ShortWriteChannel getChargePowerChannelL2() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2);
	}

	// SetPoint Channel L3
	public default void _setSymmetricEssActivePowerL3(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL3().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL3() {
		return this.getChargePowerChannelL3().value();
	}

	public default ShortWriteChannel getChargePowerChannelL3() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3);
	}

	// Getter for VE Bus State
	public default VEBusState getVEBusState() {
		return this.getVEBusStateChannel().value().asEnum();
	}

	public default Channel<VEBusState> getVEBusStateChannel() {
		return this.channel(ChannelId.VE_BUS_STATE);
	}

	// Getter for VE Bus BMS Error
	public default VEBusBMSError getVEBusBMSError() {
		return this.getVEBusBMSErrorChannel().value().asEnum();
	}

	public default Channel<VEBusBMSError> getVEBusBMSErrorChannel() {
		return this.channel(ChannelId.VE_BUS_BMS_ERROR);
	}

	// Getter for Phase Rotation Warning
	public default Alarm getPhaseRotationWarning() {
		return this.getPhaseRotationWarningChannel().value().asEnum();
	}

	public default Channel<Alarm> getPhaseRotationWarningChannel() {
		return this.channel(ChannelId.PHASE_ROTATION_WARNING);
	}

	// Getter for VE Bus Reset
	public default Value<Integer> getVEBusReset() {
		return this.getVEBusResetChannel().value();
	}

	public default IntegerReadChannel getVEBusResetChannel() {
		return this.channel(ChannelId.VE_BUS_RESET);
	}

	// Getter for VE Bus BMS Allow Battery Charge
	public default AllowDisallow getVEBusBMSAllowBatteryCharge() {
		return this.getVEBusBMSAllowBatteryChargeChannel().value().asEnum();
	}

	public default Channel<AllowDisallow> getVEBusBMSAllowBatteryChargeChannel() {
		return this.channel(ChannelId.VE_BUS_BMS_ALLOW_BATTERY_CHARGE);
	}

	// Getter for VE Bus BMS Allow Battery Discharge
	public default AllowDisallow getVEBusBMSAllowBatteryDischarge() {
		return this.getVEBusBMSAllowBatteryDischargeChannel().value().asEnum();
	}

	public default Channel<AllowDisallow> getVEBusBMSAllowBatteryDischargeChannel() {
		return this.channel(ChannelId.VE_BUS_BMS_ALLOW_BATTERY_DISCHARGE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerChannel() {
		return this.channel(ChannelId.APPARENT_POWER);
	}

	/**
	 * Gets the Apparent Power in [VA]. See {@link ChannelId#APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPower() {
		return this.getApparentPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#APPARENT_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setApparentPower(Integer value) {
		this.getApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#APPARENT_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setApparentPower(int value) {
		this.getApparentPowerChannel().setNextValue(value);
	}

	// Set useable Capacity
	public default void _setUseableCapacity(Integer value) {
		this.getUseableCapacityChannel().setNextValue(value);
	}

	public default void _setUseableCapacity(int value) {
		this.getUseableCapacityChannel().setNextValue(value);
	}

	public default Value<Integer> getUseableCapacity() {
		return this.getUseableCapacityChannel().value();
	}

	public default IntegerReadChannel getUseableCapacityChannel() {
		return this.channel(ChannelId.USEABLE_CAPACITY);
	}

	// Set useable SoC
	public default void _setUseableSoc(Integer value) {
		this.getUseableSocChannel().setNextValue(value);
	}

	public default void _setUseableSoc(int value) {
		this.getUseableSocChannel().setNextValue(value);
	}

	public default Value<Integer> getUseableSoc() {
		return this.getUseableSocChannel().value();
	}

	public default IntegerReadChannel getUseableSocChannel() {
		return this.channel(ChannelId.USEABLE_SOC);
	}	

	public void setBatteryInverter(VictronBatteryInverter batteryInverter);

	public void unsetBatteryInverter(VictronBatteryInverter batteryInverter);
	
	public void setBattery(VictronBattery battery);

	public void unsetBattery(VictronBattery battery);
	
    //public VictronBatteryInverter getBatteryInverter();



	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(VictronEss.class, accessMode, 200) //
				.channel(0, ChannelId.SET_ACTIVE_POWER_L1, ModbusType.UINT16) //
				.channel(1, ChannelId.SET_ACTIVE_POWER_L2, ModbusType.UINT16) //
				.channel(2, ChannelId.SET_ACTIVE_POWER_L3, ModbusType.UINT16) //
				.channel(3, ChannelId.CHARGE_STATE, ModbusType.UINT16) //
				.channel(4, ChannelId.BATTERY_VOLTAGE, ModbusType.UINT16) //
				.channel(5, ChannelId.BATTERY_CURRENT, ModbusType.UINT16) //
				.channel(6, ChannelId.VE_BUS_STATE, ModbusType.UINT16) //
				.channel(7, ChannelId.VE_BUS_ERROR, ModbusType.UINT16) //
				.channel(8, ChannelId.USEABLE_CAPACITY, ModbusType.UINT16) //
				.channel(9, ChannelId.USEABLE_SOC, ModbusType.UINT16) //
				.build();
	}

	public Object getPhase();



	
}
