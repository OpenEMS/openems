package io.openems.edge.batteryinverter.victron.ro;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.victron.battery.VictronBattery;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.ActiveInputSource;
import io.openems.edge.victron.enums.BatteryState;

/**
 * Victron Battery Inverter interface for Venus OS / Cerbo GX systems.
 *
 * <p>
 * This interface defines all channels for the Victron Battery Inverter
 * connected via Modbus to Venus OS / Cerbo GX. It provides system-level
 * monitoring and control of the Victron energy system including AC/DC power
 * flows, battery status, and ESS control parameters.
 *
 * @see <a href="https://github.com/victronenergy/dbus_modbustcp">Venus
 *      Modbus-TCP</a>
 */
public interface VictronBatteryInverter extends OffGridBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, OpenemsComponent, StartStoppable, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// ================= State Machine =================
		STATE_MACHINE(Doc.of(State.values())//
				.text("Current state of the component state-machine")), //
		RUN_FAILED(Doc.of(Level.FAULT)//
				.text("Running the component logic failed")), //

		// ================= System Info =================
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING)//
				.text("Serial number of the Victron system")), //

		// ================= Relay Control =================
		CCGX_RELAY1_STATE(new IntegerDoc()//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.ON_OFF)//
				.text("Cerbo GX Relay 1 state")), //
		CCGX_RELAY2_STATE(new IntegerDoc()//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.ON_OFF)//
				.text("Cerbo GX Relay 2 state")), //

		// ================= AC PV on Output (Critical Loads) =================
		AC_PV_ON_OUTPUT_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on critical loads output L1")), //
		AC_PV_ON_OUTPUT_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on critical loads output L2")), //
		AC_PV_ON_OUTPUT_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on critical loads output L3")), //

		// ================= AC PV on Input (Grid Side) =================
		AC_PV_ON_INPUT_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on grid input L1")), //
		AC_PV_ON_INPUT_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on grid input L2")), //
		AC_PV_ON_INPUT_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC PV power on grid input L3")), //

		// ================= AC Consumption =================
		AC_CONSUMPTION_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC consumption power L1")), //
		AC_CONSUMPTION_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC consumption power L2")), //
		AC_CONSUMPTION_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC consumption power L3")), //

		// ================= Grid Power =================
		GRID_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Grid power L1. Positive=Import; Negative=Export")), //
		GRID_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Grid power L2. Positive=Import; Negative=Export")), //
		GRID_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Grid power L3. Positive=Import; Negative=Export")), //

		// ================= Generator Power =================
		AC_GENSET_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC generator power L1")), //
		AC_GENSET_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC generator power L2")), //
		AC_GENSET_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("AC generator power L3")), //

		// ================= Active Input Source =================
		ACTIVE_INPUT_SOURCE(Doc.of(ActiveInputSource.values())//
				.text("Currently active AC input source")), //

		// ================= DC Battery Measurements =================
		DC_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.text("DC battery voltage")), //
		DC_BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.text("DC battery current. Positive=Discharge; Negative=Charge")), //

		// ================= Battery Status =================
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Battery state of charge")), //
		BATTERY_STATE(Doc.of(BatteryState.values())//
				.text("Battery charging/discharging state")), //
		BATTERY_CONSUMED_AMPHOURS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE_HOURS)//
				.text("Consumed amp-hours since last full charge")), //
		BATTERY_TIME_TO_GO(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Estimated time until battery empty")), //

		// ================= DC PV (MPPT Chargers) =================
		DC_PV_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Total DC PV power from MPPT chargers")), //
		DC_PV_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.text("Total DC PV current from MPPT chargers")), //

		// ================= Charger & System Power =================
		CHARGER_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("Total charger power")), //
		DC_SYSTEM_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("DC system power (DC loads)")), //

		// ================= VE.Bus Charging =================
		VE_BUS_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.text("VE.Bus charging current")), //
		VE_BUS_CHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("VE.Bus charging power")), //

		// ================= ESS Control Parameters =================
		ESS_CONTROL_LOOP_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)//
				.text("ESS control loop setpoint (16-bit)")), //
		ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)//
				.text("ESS control loop setpoint with scale factor 2")), //

		// ================= ESS Current Limits =================
		ESS_MAX_CHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.PERCENT)//
				.text("Max charge current as percentage of system max")), //
		ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.PERCENT)//
				.text("Max discharge current as percentage of system max")), //
		ESS_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)//
				.text("Maximum discharge power limit")), //
		SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.AMPERE)//
				.text("System maximum charge current")), //

		// ================= Feed-in Control =================
		MAX_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)//
				.text("Maximum grid feed-in power")), //
		FEED_EXCESS_DC(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Feed excess DC-coupled PV to grid")), //
		DONT_FEED_EXCESS_AC(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Do not feed excess AC-coupled PV to grid")), //
		PV_POWER_LIMITER_ACTIVE(Doc.of(ActiveInactive.values())//
				.text("PV power limiter currently active")), //

		// ================= Battery Voltage Control =================
		MAX_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.VOLT)//
				.text("Maximum battery charge voltage"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// ================= AC PV on Output (Critical Loads) Accessors
	// =================

	/**
	 * Gets the AC PV power on critical loads output L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnOutputPowerL1() {
		return this.getAcPvOnOutputPowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_OUTPUT_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnOutputPowerL1Channel() {
		return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L1);
	}

	/**
	 * Gets the AC PV power on critical loads output L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnOutputPowerL2() {
		return this.getAcPvOnOutputPowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_OUTPUT_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnOutputPowerL2Channel() {
		return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L2);
	}

	/**
	 * Gets the AC PV power on critical loads output L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnOutputPowerL3() {
		return this.getAcPvOnOutputPowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_OUTPUT_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnOutputPowerL3Channel() {
		return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L3);
	}

	// ================= AC PV on Input (Grid Side) Accessors =================

	/**
	 * Gets the AC PV power on grid input L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnInputPowerL1() {
		return this.getAcPvOnInputPowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_INPUT_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnInputPowerL1Channel() {
		return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L1);
	}

	/**
	 * Gets the AC PV power on grid input L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnInputPowerL2() {
		return this.getAcPvOnInputPowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_INPUT_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnInputPowerL2Channel() {
		return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L2);
	}

	/**
	 * Gets the AC PV power on grid input L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPvOnInputPowerL3() {
		return this.getAcPvOnInputPowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_PV_ON_INPUT_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPvOnInputPowerL3Channel() {
		return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L3);
	}

	// ================= AC Consumption Accessors =================

	/**
	 * Gets the AC consumption power L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcConsumptionPowerL1() {
		return this.getAcConsumptionPowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_CONSUMPTION_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcConsumptionPowerL1Channel() {
		return this.channel(ChannelId.AC_CONSUMPTION_POWER_L1);
	}

	/**
	 * Gets the AC consumption power L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcConsumptionPowerL2() {
		return this.getAcConsumptionPowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_CONSUMPTION_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcConsumptionPowerL2Channel() {
		return this.channel(ChannelId.AC_CONSUMPTION_POWER_L2);
	}

	/**
	 * Gets the AC consumption power L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcConsumptionPowerL3() {
		return this.getAcConsumptionPowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_CONSUMPTION_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcConsumptionPowerL3Channel() {
		return this.channel(ChannelId.AC_CONSUMPTION_POWER_L3);
	}

	// ================= Grid Power Accessors =================

	/**
	 * Gets the grid power L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerL1() {
		return this.getGridPowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerL1Channel() {
		return this.channel(ChannelId.GRID_POWER_L1);
	}

	/**
	 * Gets the grid power L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerL2() {
		return this.getGridPowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerL2Channel() {
		return this.channel(ChannelId.GRID_POWER_L2);
	}

	/**
	 * Gets the grid power L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerL3() {
		return this.getGridPowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerL3Channel() {
		return this.channel(ChannelId.GRID_POWER_L3);
	}

	// ================= Generator Power Accessors =================

	/**
	 * Gets the AC generator power L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcGensetPowerL1() {
		return this.getAcGensetPowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_GENSET_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcGensetPowerL1Channel() {
		return this.channel(ChannelId.AC_GENSET_POWER_L1);
	}

	/**
	 * Gets the AC generator power L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcGensetPowerL2() {
		return this.getAcGensetPowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_GENSET_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcGensetPowerL2Channel() {
		return this.channel(ChannelId.AC_GENSET_POWER_L2);
	}

	/**
	 * Gets the AC generator power L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcGensetPowerL3() {
		return this.getAcGensetPowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AC_GENSET_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcGensetPowerL3Channel() {
		return this.channel(ChannelId.AC_GENSET_POWER_L3);
	}

	// ================= DC Battery Accessors =================

	/**
	 * Gets the DC battery voltage in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcBatteryVoltage() {
		return this.getDcBatteryVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcBatteryVoltageChannel() {
		return this.channel(ChannelId.DC_BATTERY_VOLTAGE);
	}

	/**
	 * Gets the DC battery current in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcBatteryCurrent() {
		return this.getDcBatteryCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_BATTERY_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcBatteryCurrentChannel() {
		return this.channel(ChannelId.DC_BATTERY_CURRENT);
	}

	// ================= Battery Status Accessors =================

	/**
	 * Gets the battery state of charge in [%].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatterySoc() {
		return this.getBatterySocChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatterySocChannel() {
		return this.channel(ChannelId.BATTERY_SOC);
	}

	/**
	 * Gets the consumed amp-hours since last full charge in [Ah].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryConsumedAmphours() {
		return this.getBatteryConsumedAmphoursChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CONSUMED_AMPHOURS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryConsumedAmphoursChannel() {
		return this.channel(ChannelId.BATTERY_CONSUMED_AMPHOURS);
	}

	/**
	 * Gets the estimated time until battery empty in [s].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryTimeToGo() {
		return this.getBatteryTimeToGoChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_TIME_TO_GO}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryTimeToGoChannel() {
		return this.channel(ChannelId.BATTERY_TIME_TO_GO);
	}

	// ================= DC PV Accessors =================

	/**
	 * Gets the total DC PV power from MPPT chargers in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPvPower() {
		return this.getDcPvPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_PV_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPvPowerChannel() {
		return this.channel(ChannelId.DC_PV_POWER);
	}

	/**
	 * Gets the total DC PV current from MPPT chargers in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPvCurrent() {
		return this.getDcPvCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_PV_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPvCurrentChannel() {
		return this.channel(ChannelId.DC_PV_CURRENT);
	}

	// ================= Charger & System Power Accessors =================

	/**
	 * Gets the total charger power in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargerPower() {
		return this.getChargerPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGER_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargerPowerChannel() {
		return this.channel(ChannelId.CHARGER_POWER);
	}

	/**
	 * Gets the DC system power (DC loads) in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcSystemPower() {
		return this.getDcSystemPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_SYSTEM_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcSystemPowerChannel() {
		return this.channel(ChannelId.DC_SYSTEM_POWER);
	}

	// ================= VE.Bus Charging Accessors =================

	/**
	 * Gets the VE.Bus charging current in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVeBusChargeCurrent() {
		return this.getVeBusChargeCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#VE_BUS_CHARGE_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVeBusChargeCurrentChannel() {
		return this.channel(ChannelId.VE_BUS_CHARGE_CURRENT);
	}

	/**
	 * Gets the VE.Bus charging power in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVeBusChargePower() {
		return this.getVeBusChargePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#VE_BUS_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVeBusChargePowerChannel() {
		return this.channel(ChannelId.VE_BUS_CHARGE_POWER);
	}

	// ================= ESS Control Accessors =================

	/**
	 * Gets the ESS control loop setpoint in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssControlLoopSetpoint() {
		return this.getEssControlLoopSetpointChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_CONTROL_LOOP_SETPOINT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssControlLoopSetpointChannel() {
		return this.channel(ChannelId.ESS_CONTROL_LOOP_SETPOINT);
	}

	/**
	 * Gets the max charge current as percentage of system max in [%].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMaxChargeCurrentPercentage() {
		return this.getEssMaxChargeCurrentPercentageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MAX_CHARGE_CURRENT_PERCENTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMaxChargeCurrentPercentageChannel() {
		return this.channel(ChannelId.ESS_MAX_CHARGE_CURRENT_PERCENTAGE);
	}

	/**
	 * Gets the max discharge current as percentage of system max in [%].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMaxDischargeCurrentPercentage() {
		return this.getEssMaxDischargeCurrentPercentageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMaxDischargeCurrentPercentageChannel() {
		return this.channel(ChannelId.ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE);
	}

	/**
	 * Gets the ESS control loop setpoint with scale factor 2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssControlLoopSetpointScaleFactor2() {
		return this.getEssControlLoopSetpointScaleFactor2Channel().value();
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssControlLoopSetpointScaleFactor2Channel() {
		return this.channel(ChannelId.ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2);
	}

	/**
	 * Gets the maximum discharge power limit in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMaxDischargePower() {
		return this.getEssMaxDischargePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MAX_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMaxDischargePowerChannel() {
		return this.channel(ChannelId.ESS_MAX_DISCHARGE_POWER);
	}

	/**
	 * Gets the system maximum charge current in [A].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSystemMaxChargeCurrent() {
		return this.getSystemMaxChargeCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SYSTEM_MAX_CHARGE_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSystemMaxChargeCurrentChannel() {
		return this.channel(ChannelId.SYSTEM_MAX_CHARGE_CURRENT);
	}

	// ================= Feed-in Control Accessors =================

	/**
	 * Gets the maximum grid feed-in power in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxFeedInPower() {
		return this.getMaxFeedInPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_FEED_IN_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxFeedInPowerChannel() {
		return this.channel(ChannelId.MAX_FEED_IN_POWER);
	}

	// ================= Battery Voltage Control Accessors =================

	/**
	 * Gets the maximum battery charge voltage in [V].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargeVoltage() {
		return this.getMaxChargeVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CHARGE_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargeVoltageChannel() {
		return this.channel(ChannelId.MAX_CHARGE_VOLTAGE);
	}

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

	/**
	 * Gets the maximum charge power in [W].
	 *
	 * @return the max charge power or null if not available
	 */
	public Integer getMaxChargePower();

	/**
	 * Gets the maximum discharge power in [W].
	 *
	 * @return the max discharge power or null if not available
	 */
	public Integer getMaxDischargePower();

	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Gets the Modbus slave nature table for this component.
	 *
	 * @param accessMode the access mode filter
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(VictronBatteryInverter.class, accessMode, 200) //
				.channel(0, ChannelId.ESS_MAX_CHARGE_CURRENT_PERCENTAGE, ModbusType.UINT16) //
				.channel(1, ChannelId.ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE, ModbusType.UINT16) //
				.channel(2, ChannelId.ESS_MAX_DISCHARGE_POWER, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Calculates hardware limits from battery and inverter.
	 *
	 * @return true if limits were successfully calculated
	 */
	public boolean calculateHardwareLimits();

}
