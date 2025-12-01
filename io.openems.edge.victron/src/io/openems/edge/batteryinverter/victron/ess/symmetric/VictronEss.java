package io.openems.edge.batteryinverter.victron.ess.symmetric;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.victron.VictronBattery;
import io.openems.edge.batteryinverter.victron.ro.VictronBatteryInverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.ShortWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.ActiveInputSource;
import io.openems.edge.victron.enums.Alarm;
import io.openems.edge.victron.enums.AllowDisallow;
import io.openems.edge.victron.enums.ChargeStateEss;
import io.openems.edge.victron.enums.EnableDisable;
import io.openems.edge.victron.enums.ErrorYesNo;
import io.openems.edge.victron.enums.SwitchPosition;
import io.openems.edge.victron.enums.VeBusBmsError;
import io.openems.edge.victron.enums.VeBusError;
import io.openems.edge.victron.enums.VeBusState;

/**
 *
 * This interface defines all channels for the Victron Energy Storage System
 * connected via Modbus to GX. It supports both single-phase
 * and three-phase configurations.
 *
 * <p>Modbus registers are based on Victron´s Modbus-TCP documentation.
 *
 * @see <a href="https://github.com/victronenergy/dbus_modbustcp/blob/master/CCGX-Modbus-TCP-register-list.xlsx">GX Modbus-TCP list</a>
 */
public interface VictronEss extends OpenemsComponent, EventHandler, ModbusComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// ================= Power Setpoints =================
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		SET_ACTIVE_POWER(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		SET_REACTIVE_POWER(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// ================= AC Input Measurements (Grid Side) =================
		VOLTAGE_INPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_INPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_INPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		CURRENT_INPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_INPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_INPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		FREQUENCY_INPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //
		FREQUENCY_INPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //
		FREQUENCY_INPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //

		ACTIVE_POWER_INPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_INPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_INPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		// ================= AC Output Measurements (Load Side) =================
		VOLTAGE_OUTPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_OUTPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_OUTPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		CURRENT_OUTPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_OUTPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_OUTPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		FREQUENCY_OUTPUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //

		ACTIVE_POWER_OUTPUT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_OUTPUT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_OUTPUT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		// ================= Battery Measurements =================
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		CURRENT_INPUT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * DC Discharge Power.
		 *
		 * <p>Actual DC-side battery discharge power. Negative values for charge;
		 * positive for discharge. This is the power actually going into/out of
		 * the battery, not including inverter losses.
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("DC battery power. Negative=Charge; Positive=Discharge")), //

		/**
		 * Cumulated DC Charge Energy.
		 */
		DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Cumulated DC Discharge Energy.
		 */
		DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// ================= System Status =================
		PHASE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //

		ACTIVE_INPUT(Doc.of(ActiveInputSource.values()) //
				.text("0=AC Input 1; 1=AC Input 2")), //

		VE_BUS_STATE(Doc.of(VeBusState.values()) //
				.text("VE.Bus system state")), //

		VE_BUS_ERROR(Doc.of(VeBusError.values()) //
				.text("VE.Bus error code")), //

		SWITCH_POSITION(Doc.of(SwitchPosition.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("1=Charger Only; 2=Inverter Only; 3=On; 4=Off")), //

		// ================= Alarms =================
		TEMPERATURE_ALARM(Doc.of(Alarm.values()) //
				.text("0=Ok; 1=Warning; 2=Alarm")), //
		LOW_BATTERY_ALARM(Doc.of(Alarm.values()) //
				.text("0=Ok; 1=Warning; 2=Alarm")), //
		OVERLOAD_ALARM(Doc.of(Alarm.values()) //
				.text("0=Ok; 1=Warning; 2=Alarm")), //

		// ================= ESS Power Setpoints (16-bit) =================
		ESS_POWER_SETPOINT_PHASE_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		ESS_POWER_SETPOINT_PHASE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		ESS_POWER_SETPOINT_PHASE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// ================= ESS Power Setpoints (32-bit signed) =================
		INT32_ESS_POWER_SETPOINT_PHASE_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INT32_ESS_POWER_SETPOINT_PHASE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INT32_ESS_POWER_SETPOINT_PHASE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// ================= ESS Control Flags =================
		/**
		 * Disable Charge Flag (inverse logic!).
		 *
		 * <p>0 = Charge allowed; 1 = Charge DISABLED.
		 */
		ESS_DISABLE_CHARGE_FLAG(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("0=Charge allowed; 1=Charge DISABLED (inverse logic)")), //

		/**
		 * Disable Feedback/Discharge Flag (inverse logic!).
		 *
		 * <p>0 = Feed-in allowed; 1 = Feed-in DISABLED.
		 */
		ESS_DISABLE_FEEDBACK_FLAG(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("0=Feed-in allowed; 1=Feed-in DISABLED (inverse logic)")), //

		// ================= Per-Phase Alarms =================
		TEMPERATURE_SENSOR_ALARM(Doc.of(Alarm.values())), //
		TEMPERATURE_ALARM_L1(Doc.of(Alarm.values())), //
		TEMPERATURE_ALARM_L2(Doc.of(Alarm.values())), //
		TEMPERATURE_ALARM_L3(Doc.of(Alarm.values())), //

		LOW_BATTERY_ALARM_L1(Doc.of(Alarm.values())), //
		LOW_BATTERY_ALARM_L2(Doc.of(Alarm.values())), //
		LOW_BATTERY_ALARM_L3(Doc.of(Alarm.values())), //

		OVERLOAD_ALARM_L1(Doc.of(Alarm.values())), //
		OVERLOAD_ALARM_L2(Doc.of(Alarm.values())), //
		OVERLOAD_ALARM_L3(Doc.of(Alarm.values())), //

		RIPPLE_ALARM_L1(Doc.of(Alarm.values())), //
		RIPPLE_ALARM_L2(Doc.of(Alarm.values())), //
		RIPPLE_ALARM_L3(Doc.of(Alarm.values())), //

		VOLTAGE_SENSOR_ALARM(Doc.of(Alarm.values())), //
		PHASE_ROTATION_WARNING(Doc.of(Alarm.values()) //
				.text("Phase rotation incorrect")), //

		// ================= VE.Bus BMS Status =================
		VE_BUS_BMS_EXPECTED(Doc.of(ErrorYesNo.values()) //
				.text("0=No; 1=Yes")), //
		VE_BUS_BMS_ERROR(Doc.of(ErrorYesNo.values()) //
				.text("0=No; 1=Yes")), //
		VE_BUS_BMS_ALLOW_BATTERY_CHARGE(Doc.of(AllowDisallow.values()) //
				.text("0=Disallow; 1=Allow")), //
		VE_BUS_BMS_ALLOW_BATTERY_DISCHARGE(Doc.of(AllowDisallow.values()) //
				.text("0=Disallow; 1=Allow")), //
		VE_BUS_RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Write 1 to reset VE.Bus")), //

		// ================= PV & Solar Settings =================
		DISABLE_PV_INVERTER(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("0=PV enabled; 1=PV disabled")), //
		SOLAR_OFFSET_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("0=1V offset; 1=0.1V offset for overvoltage feed-in")), //

		// ================= Other Status =================
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		SUSTAIN_ACTIVE(Doc.of(ActiveInactive.values()) //
				.text("Sustain mode active")), //

		// ================= Energy Metering =================
		ENERGY_FROM_AC_IN_1_TO_AC_OUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_IN_1_TO_BATTERY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_IN_2_TO_AC_OUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_IN_2_TO_BATTERY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_OUT_TO_AC_IN_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_OUT_TO_AC_IN_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_BATTERY_TO_AC_IN_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_BATTERY_TO_AC_IN_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_BATTERY_TO_AC_OUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ENERGY_FROM_AC_OUT_TO_BATTERY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// ================= Charging State =================
		CHARGE_STATE(Doc.of(ChargeStateEss.values()) //
				.text("Battery charging state")), //
		LOW_CELL_VOLTAGE_IMMINENT(Doc.of(Alarm.values()) //
				.text("0=Ok; 1=Warning - cell voltage low")), //

		// ================= Generator Control =================
		PREFER_RENEWABLE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELECT_REMOTE_GENERATOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE)), //
		REMOTE_GENERATOR_SELECTED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //

		// ================= Overvoltage Feed-in Settings =================
		FEED_DC_OVERVOLTAGE_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.text("0=Feed-in; 1=Do not feed-in")), //
		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_POWER_SETPOINT_AS_FEED_IN_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("0=Normal; 1=Setpoint is feed-in limit")), //

		// ================= AC Input Status =================
		AC_INPUT1_IGNORED(Doc.of(EnableDisable.values()) //
				.text("0=Active; 1=Ignored")), //
		AC_INPUT2_IGNORED(Doc.of(EnableDisable.values()) //
				.text("0=Active; 1=Ignored")), //
		GRID_LOST_ALARM(Doc.of(Alarm.values()) //
				.text("0=Ok; 2=Grid lost")), //

		// ================= Useable Capacity & SoC =================
		/**
		 * Useable battery capacity in Wh.
		 *
		 * <p>This is the capacity available for use, excluding emergency reserves
		 * from controllers like EmergencyCapacityReserve and LimitTotalDischarge.
		 */
		USEABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Useable State of Charge in %.
		 *
		 * <p>This is the SoC available for use, with controller reserves
		 * (EmergencyCapacityReserve, LimitTotalDischarge) already subtracted.
		 */
		USEABLE_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the Channel for ActivePower on AC In L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerInputL1Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_INPUT_L1);
	}

	/**
	 * Gets the ActivePower on AC In L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerInputL1() {
		return this.getActivePowerInputL1Channel().value();
	}	
	

	/**
	 * Gets the Channel for ActivePower on AC In L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerInputL2Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_INPUT_L2);
	}

	/**
	 * Gets the ActivePower on AC In L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerInputL2() {
		return this.getActivePowerInputL2Channel().value();
	}	
	

	/**
	 * Gets the Channel for ActivePower on AC In L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerInputL3Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_INPUT_L3);
	}

	/**
	 * Gets the ActivePower on AC In L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerInputL3() {
		return this.getActivePowerInputL3Channel().value();
	}	
		

	/**
	 * Gets the Channel for Voltage on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageInputL1Channel() {
		return this.channel(ChannelId.VOLTAGE_INPUT_L1);
	}

	/**
	 * Gets the Voltage on L1 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageInputL1() {
		return this.getVoltageInputL1Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageInputL2Channel() {
		return this.channel(ChannelId.VOLTAGE_INPUT_L2);
	}

	/**
	 * Gets the Voltage on L2 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageInputL2() {
		return this.getVoltageInputL2Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageInputL3Channel() {
		return this.channel(ChannelId.VOLTAGE_INPUT_L3);
	}

	/**
	 * Gets the Voltage on L3 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageInputL3() {
		return this.getVoltageInputL3Channel().value();
	}

	/**
	 * Gets the Channel for Current on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentInputL1Channel() {
		return this.channel(ChannelId.CURRENT_INPUT_L1);
	}

	/**
	 * Gets the Current on L1 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentInputL1() {
		return this.getCurrentInputL1Channel().value();
	}

	/**
	 * Gets the Channel for Current on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentInputL2Channel() {
		return this.channel(ChannelId.CURRENT_INPUT_L2);
	}

	/**
	 * Gets the Current on L2 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentInputL2() {
		return this.getCurrentInputL2Channel().value();
	}

	/**
	 * Gets the Channel for Current on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentInputL3Channel() {
		return this.channel(ChannelId.CURRENT_INPUT_L3);
	}

	/**
	 * Gets the Current on L3 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentInputL3() {
		return this.getCurrentInputL3Channel().value();
	}

	// ****************** AC Out

	/**
	 * Gets the Channel for ActivePower on AC Out L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerOutputL1Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_OUTPUT_L1);
	}

	/**
	 * Gets the ActivePower on AC In L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerOutputL1() {
		return this.getActivePowerOutputL1Channel().value();
	}	
	

	/**
	 * Gets the Channel for ActivePower on AC In L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerOutputL2Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_OUTPUT_L2);
	}

	/**
	 * Gets the ActivePower on AC In L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerOutputL2() {
		return this.getActivePowerOutputL2Channel().value();
	}	
	

	/**
	 * Gets the Channel for ActivePower on AC In L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerOutputL3Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_OUTPUT_L3);
	}

	/**
	 * Gets the ActivePower on AC In L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerOutputL3() {
		return this.getActivePowerOutputL3Channel().value();
	}	
	
	
	

	/**
	 * Gets the Channel for Voltage on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageOutputL1Channel() {
		return this.channel(ChannelId.VOLTAGE_OUTPUT_L1);
	}

	/**
	 * Gets the Voltage on L1 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageOutputL1() {
		return this.getVoltageOutputL1Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageOutputL2Channel() {
		return this.channel(ChannelId.VOLTAGE_OUTPUT_L2);
	}

	/**
	 * Gets the Voltage on L2 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageOutputL2() {
		return this.getVoltageOutputL2Channel().value();
	}

	/**
	 * Gets the Channel for Voltage on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageOutputL3Channel() {
		return this.channel(ChannelId.VOLTAGE_OUTPUT_L3);
	}

	/**
	 * Gets the Voltage on L3 in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageOutputL3() {
		return this.getVoltageOutputL3Channel().value();
	}

	/**
	 * Gets the Channel for Current on L1.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentOutputL1Channel() {
		return this.channel(ChannelId.CURRENT_OUTPUT_L1);
	}

	/**
	 * Gets the Current on L1 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentOutputL1() {
		return this.getCurrentOutputL1Channel().value();
	}

	/**
	 * Gets the Channel for Current on L2.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentOutputL2Channel() {
		return this.channel(ChannelId.CURRENT_OUTPUT_L2);
	}

	/**
	 * Gets the Current on L2 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentOutputL2() {
		return this.getCurrentOutputL2Channel().value();
	}

	/**
	 * Gets the Channel for Current on L3.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentOutputL3Channel() {
		return this.channel(ChannelId.CURRENT_OUTPUT_L3);
	}
	
	
	/**
	 * Gets the Current on L2 in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentOutputL3() {
		return this.getCurrentOutputL3Channel().value();
	}	

	/**
	 * Sets the active power.
	 *
	 * @param value the power value in [W]
	 * @throws OpenemsNamedException on error
	 */
	public default void _setActivePower(Short value) throws OpenemsNamedException {
		this.getChargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the charge power.
	 *
	 * @return the charge power value
	 */
	public default Value<Short> getChargePower() {
		return this.getChargePowerChannel().value();
	}

	/**
	 * Gets the charge power channel.
	 *
	 * @return the channel
	 */
	public default ShortWriteChannel getChargePowerChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER);
	}

	/**
	 * Sets the DC discharge power.
	 *
	 * @param value the power value in [W]
	 */
	public default void _setDcDischargePower(Integer value) {
		this.getDcDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the DC discharge power.
	 *
	 * @return the power value
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	/**
	 * Gets the DC discharge power channel.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Sets the DC discharge energy.
	 *
	 * @param value the energy value in [Wh]
	 */
	public default void _setDcDischargeEnergy(Long value) {
		this.getDcDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the DC discharge energy.
	 *
	 * @return the energy value
	 */
	public default Value<Long> getDcDischargeEnergy() {
		return this.getDcDischargeEnergyChannel().value();
	}

	/**
	 * Gets the DC discharge energy channel.
	 *
	 * @return the channel
	 */
	public default LongReadChannel getDcDischargeEnergyChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_ENERGY);
	}

	/**
	 * Sets the DC charge energy.
	 *
	 * @param value the energy value in [Wh]
	 */
	public default void _setDcChargeEnergy(Long value) {
		this.getDcChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the DC charge energy.
	 *
	 * @return the energy value
	 */
	public default Value<Long> getDcChargeEnergy() {
		return this.getDcChargeEnergyChannel().value();
	}

	/**
	 * Gets the DC charge energy channel.
	 *
	 * @return the channel
	 */
	public default LongReadChannel getDcChargeEnergyChannel() {
		return this.channel(ChannelId.DC_CHARGE_ENERGY);
	}		
	
	// SetPoint Channel L1
	public default void setEssActivePowerL1(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL1().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL1() {
		return this.getChargePowerChannelL1().value();
	}

	public default ShortWriteChannel getChargePowerChannelL1() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1);
	}

	// SetPoint Channel L2
	public default void setEssActivePowerL2(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL2().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL2() {
		return this.getChargePowerChannelL2().value();
	}

	public default ShortWriteChannel getChargePowerChannelL2() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2);
	}

	// SetPoint Channel L3
	public default void setEssActivePowerL3(Short value) throws OpenemsNamedException {
		this.getChargePowerChannelL3().setNextWriteValue(value);
	}

	public default Value<Short> getChargePowerL3() {
		return this.getChargePowerChannelL3().value();
	}

	public default ShortWriteChannel getChargePowerChannelL3() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3);
	}

	/**
	 * Sets the disable charge flag.
	 *
	 * <p>0 = Charging enabled; 1 = Charging DISABLED (inverse logic).
	 *
	 * @param value the {@link EnableDisable} value
	 * @throws OpenemsNamedException on error
	 */
	public default void _setDisableChargeFlag(EnableDisable value) throws OpenemsNamedException {
		this.setDisableChargeFlagChannel().setNextWriteValue(value);
	}

	public default EnableDisable getDisableChargeFlag() {
		return this.getDisableChargeFlagChannel().value().asEnum();
	}

	public default Channel<EnableDisable> getDisableChargeFlagChannel() {
		return this.channel(ChannelId.ESS_DISABLE_CHARGE_FLAG);
	}

	public default WriteChannel<EnableDisable> setDisableChargeFlagChannel() {
		return this.channel(ChannelId.ESS_DISABLE_CHARGE_FLAG);
	}

	/**
	 * Sets the disable discharge flag.
	 *
	 * <p>0 = Discharging enabled; 1 = Discharging DISABLED (inverse logic).
	 *
	 * @param value the {@link EnableDisable} value
	 * @throws OpenemsNamedException on error
	 */
	public default void _setDisableDischargeFlag(EnableDisable value) throws OpenemsNamedException {
		this.setDisableDischargeFlagChannel().setNextWriteValue(value);
	}

	public default EnableDisable getDisableDischargeFlag() {
		return this.getDisableDischargeFlagChannel().value().asEnum();
	}

	public default Channel<EnableDisable> getDisableDischargeFlagChannel() {
		return this.channel(ChannelId.ESS_DISABLE_FEEDBACK_FLAG);
	}

	public default WriteChannel<EnableDisable> setDisableDischargeFlagChannel() {
		return this.channel(ChannelId.ESS_DISABLE_FEEDBACK_FLAG);
	}		
	
	

	/**
	 * Gets the VE Bus State.
	 *
	 * @return the VE Bus State
	 */
	public default VeBusState getVeBusState() {
		return this.getVeBusStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for VE Bus State.
	 *
	 * @return the Channel
	 */
	public default Channel<VeBusState> getVeBusStateChannel() {
		return this.channel(ChannelId.VE_BUS_STATE);
	}

	/**
	 * Gets the VE Bus BMS Error.
	 *
	 * @return the VE Bus BMS Error
	 */
	public default VeBusBmsError getVeBusBmsError() {
		return this.getVeBusBmsErrorChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for VE Bus BMS Error.
	 *
	 * @return the Channel
	 */
	public default Channel<VeBusBmsError> getVeBusBmsErrorChannel() {
		return this.channel(ChannelId.VE_BUS_BMS_ERROR);
	}

	// Getter for Phase Rotation Warning
	public default Alarm getPhaseRotationWarning() {
		return this.getPhaseRotationWarningChannel().value().asEnum();
	}

	public default Channel<Alarm> getPhaseRotationWarningChannel() {
		return this.channel(ChannelId.PHASE_ROTATION_WARNING);
	}

	/**
	 * Gets the VE Bus Reset.
	 *
	 * @return the VE Bus Reset value
	 */
	public default Value<Integer> getVeBusReset() {
		return this.getVeBusResetChannel().value();
	}

	/**
	 * Gets the Channel for VE Bus Reset.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVeBusResetChannel() {
		return this.channel(ChannelId.VE_BUS_RESET);
	}

	/**
	 * Gets VE Bus BMS Allow Battery Charge.
	 *
	 * @return the AllowDisallow value
	 */
	public default AllowDisallow getVeBusBmsAllowBatteryCharge() {
		return this.getVeBusBmsAllowBatteryChargeChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for VE Bus BMS Allow Battery Charge.
	 *
	 * @return the Channel
	 */
	public default Channel<AllowDisallow> getVeBusBmsAllowBatteryChargeChannel() {
		return this.channel(ChannelId.VE_BUS_BMS_ALLOW_BATTERY_CHARGE);
	}

	/**
	 * Gets VE Bus BMS Allow Battery Discharge.
	 *
	 * @return the AllowDisallow value
	 */
	public default AllowDisallow getVeBusBmsAllowBatteryDischarge() {
		return this.getVeBusBmsAllowBatteryDischargeChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for VE Bus BMS Allow Battery Discharge.
	 *
	 * @return the Channel
	 */
	public default Channel<AllowDisallow> getVeBusBmsAllowBatteryDischargeChannel() {
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

	/**
	 * Sets the useable capacity.
	 *
	 * @param value the capacity value in [Wh]
	 */
	public default void _setUseableCapacity(Integer value) {
		this.getUseableCapacityChannel().setNextValue(value);
	}

	/**
	 * Sets the useable capacity.
	 *
	 * @param value the capacity value in [Wh]
	 */
	public default void _setUseableCapacity(int value) {
		this.getUseableCapacityChannel().setNextValue(value);
	}

	/**
	 * Gets the useable capacity.
	 *
	 * @return the capacity value
	 */
	public default Value<Integer> getUseableCapacity() {
		return this.getUseableCapacityChannel().value();
	}

	/**
	 * Gets the useable capacity channel.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getUseableCapacityChannel() {
		return this.channel(ChannelId.USEABLE_CAPACITY);
	}

	/**
	 * Sets the useable SoC.
	 *
	 * @param value the SoC value in [%]
	 */
	public default void _setUseableSoc(Integer value) {
		this.getUseableSocChannel().setNextValue(value);
	}

	/**
	 * Sets the useable SoC.
	 *
	 * @param value the SoC value in [%]
	 */
	public default void _setUseableSoc(int value) {
		this.getUseableSocChannel().setNextValue(value);
	}

	/**
	 * Gets the useable SoC.
	 *
	 * @return the SoC value
	 */
	public default Value<Integer> getUseableSoc() {
		return this.getUseableSocChannel().value();
	}

	/**
	 * Gets the useable SoC channel.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getUseableSocChannel() {
		return this.channel(ChannelId.USEABLE_SOC);
	}

	/**
	 * Sets the battery inverter reference.
	 *
	 * @param batteryInverter the {@link VictronBatteryInverter}
	 */
	public void setBatteryInverter(VictronBatteryInverter batteryInverter);

	/**
	 * Unsets the battery inverter reference.
	 *
	 * @param batteryInverter the {@link VictronBatteryInverter}
	 */
	public void unsetBatteryInverter(VictronBatteryInverter batteryInverter);

	/**
	 * Sets the battery reference.
	 *
	 * @param battery the {@link VictronBattery}
	 */
	public void setBattery(VictronBattery battery);

	/**
	 * Unsets the battery reference.
	 *
	 * @param battery the {@link VictronBattery}
	 */
	public void unsetBattery(VictronBattery battery);

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
				.channel(10, ChannelId.TEMPERATURE_ALARM, ModbusType.UINT16) //
				.channel(11, ChannelId.TEMPERATURE_ALARM_L1, ModbusType.UINT16) //
				.channel(12, ChannelId.TEMPERATURE_ALARM_L2, ModbusType.UINT16) //
				.channel(13, ChannelId.TEMPERATURE_ALARM_L3, ModbusType.UINT16) //
				.channel(14, ChannelId.LOW_BATTERY_ALARM, ModbusType.UINT16) //
				.channel(15, ChannelId.LOW_BATTERY_ALARM_L1, ModbusType.UINT16) //
				.channel(16, ChannelId.LOW_BATTERY_ALARM_L2, ModbusType.UINT16) //
				.channel(17, ChannelId.LOW_BATTERY_ALARM_L3, ModbusType.UINT16) //
				.channel(18, ChannelId.OVERLOAD_ALARM, ModbusType.UINT16) //
				.channel(19, ChannelId.OVERLOAD_ALARM_L1, ModbusType.UINT16) //
				.channel(20, ChannelId.OVERLOAD_ALARM_L2, ModbusType.UINT16) //
				.channel(21, ChannelId.OVERLOAD_ALARM_L3, ModbusType.UINT16) //
				.channel(22, ChannelId.RIPPLE_ALARM_L1, ModbusType.UINT16) //
				.channel(23, ChannelId.RIPPLE_ALARM_L2, ModbusType.UINT16) //
				.channel(24, ChannelId.RIPPLE_ALARM_L3, ModbusType.UINT16) //

				.build();
	}

	/**
	 * Gets the phase the ESS is connected to.
	 *
	 * @return the {@link SinglePhase}
	 */
	public SinglePhase getPhase();

}
