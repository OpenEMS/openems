package io.openems.edge.battery.pylontech.powercubem2;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.DEGREE_CELSIUS;
import static io.openems.common.channel.Unit.KILOOHM;
import static io.openems.common.channel.Unit.NONE;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.battery.pylontech.powercubem2.PylontechPowercubeM2BatteryImpl.UPDATE_NUMBER_OF_PILES_CALLBACK;

import io.openems.common.channel.Level;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface PylontechPowercubeM2Battery extends Battery, OpenemsComponent, StartStoppable {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// / 3.2 Equipment Information
		VERSION_STRING(Doc.of(STRING) //
				.accessMode(READ_ONLY) //
				.text("Pylontech BMS version number")),
		PYLONTECH_INTERNAL_VERSION_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Pylontech internal version number")),
		SYSTEM_NUMBER_OF_PARALLEL_PILES(Doc.of(INTEGER) //
				.unit(NONE) //
				.accessMode(READ_ONLY) //
				.text("Number of parallel piles in system")),

		// 3.3 Remote Control Information
		// TODO: This is the most important one as it is how we tell the battery to
		// charge/discharge/sleep
		SLEEP_WAKE_CHANNEL(Doc.of(INTEGER) //
				.accessMode(READ_WRITE) //
				.text("Sleep/wake channel. Send '0xAA' to sleep, '0x55' to wake")),

		/*
		 * Pylontech's Modbus protocol refers to several states (e.g low temperature)
		 * where the battery still functions (at reduced C rate) as 'Alarms'. In this
		 * implementation we are calling them 'Warnings' as an alarm state is usually
		 * something that stops the battery as understood by the OpenEMS community.
		 * 
		 * Pylontech's protocol also defines 'Protection' states. This is where the
		 * operational range is exceeded and the main contactor is opened (disconnecting
		 * the battery). For now we are calling these 'Protection'.
		 */
		// 3.4 System Information
		BASIC_STATUS(Doc.of(Status.values()) //
				.accessMode(READ_ONLY) //
				.text("System status. 00=Sleep, 01=Charge, 02=Discharge")),
		SYSTEM_ERROR_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("System error protection (0: Normal, 1: Protect)")),
		SYSTEM_CURRENT_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Current Protection active")),
		SYSTEM_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Voltage Protection Active.")),
		SYSTEM_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Temperature Protection Active")),
		SYSTEM_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Voltage Warning")),
		SYSTEM_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Current Warning")),
		SYSTEM_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Temperature Warning")),
		SYSTEM_IDLE_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("System idle status")),
		SYSTEM_CHARGE_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("System charge status")),
		SYSTEM_DISCHARGE_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("System discharge status")),
		SYSTEM_SLEEP_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("System sleep status")),
		SYSTEM_FAN_WARN(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("System fan warning")),
		BATTERY_CELL_UNDER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Battery cell under voltage protection")),
		BATTERY_CELL_OVER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Battery cell over voltage protection")),
		PILE_UNDER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Pile under voltage protection")),
		PILE_OVER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Pile over voltage protection")),
		CHARGE_UNDER_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Charge under temperature protection")),
		CHARGE_OVER_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Charge over temperature protection")),
		DISCHARGE_UNDER_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Discharge under temperature protection")),
		DISCHARGE_OVER_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Discharge over temperature protection")),
		CHARGE_OVER_CURRENT_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Charge over current protection")),
		DISCHARGE_OVER_CURRENT_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Discharge over current protection")),
		SHORT_CIRCUIT_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Short circuit protection")),
		MODULE_OVER_TEMPERATURE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Module over temperature protection")),
		MODULE_UNDER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Module under voltage protection")),
		MODULE_OVER_VOLTAGE_PROTECTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Module over voltage protection")),
		BATTERY_CELL_LOW_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Battery cell low voltage warning")),
		BATTERY_CELL_HIGH_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Battery cell high voltage warning")),
		PILE_LOW_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Pile low voltage warning")),
		PILE_HIGH_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Pile high voltage warning")),
		CHARGE_LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Charge low temperature warning")),
		CHARGE_HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Charge high temperature warning")),
		DISCHARGE_LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Discharge low temperature warning")),
		DISCHARGE_HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Discharge high temperature warning")),
		CHARGE_OVER_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Charge over current warning")),
		DISCHARGE_OVER_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Disharge over current warning")),
		BMS_HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Main controller (BMS) high temperature warning")),
		MODULE_HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Module high temperature warning")),
		MODULE_LOW_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Module low voltage warning")),
		MODULE_HIGH_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.accessMode(READ_ONLY) //
				.text("Module high voltage warning")),
		SYSTEM_TEMPERATURE(Doc.of(INTEGER) //
				.unit(DEGREE_CELSIUS) //
				.accessMode(READ_ONLY) //
				.text("Temperature")),
		CYCLE_TIMES(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Cycle times")),
		DISCHARGE_CIRCUIT_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Discharge circuit active")),
		CHARGE_CIRCUIT_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Charge circuit active")),
		PRE_CHARGE_CIRCUIT_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Pre-charge circuit active")),
		BUZZER_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Buzzer active")),
		HEATING_FILM_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Heating film active")),
		CURRENT_LIMITING_MODULE_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Current limiting module active")),
		FAN_ACTIVE(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Fan active")),
		MAX_VOLTAGE_CELL_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Cell number of cell with max voltage")),
		MIN_VOLTAGE_CELL_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Cell number of cell with min voltage")),
		MAX_TEMPERATURE_CELL_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Cell number of cell with max temperature")),
		MIN_TEMPERATURE_CELL_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Cell number of cell with min temperature")),
		MAX_MODULE_VOLTAGE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(VOLT) //
				.text("Max module voltage (V)")),
		MIN_MODULE_VOLTAGE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(VOLT) //
				.text("Min module voltage (V)")),
		MAX_VOLTAGE_MODULE_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Module number of module with max voltage")),
		MIN_VOLTAGE_MODULE_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Module number of module with min voltage")),
		MAX_MODULE_TEMPERATURE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(DEGREE_CELSIUS) //
				.text("Max module temperature (oC)")),
		MIN_MODULE_TEMPERATURE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(DEGREE_CELSIUS) //
				.text("Min module temperature (oC)")),
		MAX_TEMPERATURE_MODULE_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Module number of module with max temperature")),
		MIN_TEMPERATURE_MODULE_NUMBER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Module number of module with min temperature")),
		REMAINING_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Remaining capacity (Wh)")),
		CHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Charge capacity in this cycle (Wh)")),
		DISCHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Discharge capacity in this cycle (Wh)")),
		DAILY_ACCUMULATED_CHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Daily accumulated charge capacity (Wh)")),
		DAILY_ACCUMULATED_DISCHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Daily accumulated discharge capacity (Wh)")),
		HISTORICAL_ACCUMULATED_CHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Historical accumulated charge capacity (Wh)")),
		HISTORICAL_ACCUMULATED_DISCHARGE_CAPACITY(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT_HOURS) //
				.text("Historical accumulated discharge capacity (Wh)")),
		REQUEST_FORCE_CHARGE_MARK(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Force charge requested (1=yes, 0=no)")),

		// TODO: Clarify w Pylontech
		REQUEST_BALANCE_CHARGE_MARK(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Balance charge requested (1=yes, 0=no)")),

		// TODO: Clarify w Pylontech
		NUMBER_OF_PILES_IN_PARALLEL(new IntegerDoc() //
				.accessMode(READ_ONLY) //
				.text("Number of piles in parallel (max 32)") //
				.onInit(UPDATE_NUMBER_OF_PILES_CALLBACK)),
		VOLTAGE_SENSOR_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Voltage sensor error")),
		TEMPERATURE_SENSOR_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Temperature sensor error")),
		INTERNAL_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Internal communication error")),
		INPUT_OVERVOLTAGE_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Input overvoltage error")),
		INPUT_TRANSPOSITION_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Input transposition error (INPUT RV ERR)")),
		RELAY_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Relay error")),
		BATTERY_DAMAGE_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Battery Damage error")),
		SWITCH_OFF_CIRCUIT_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Switch off circuit error")),
		BMIC_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("BMIC error")),
		INTERNAL_BUS_ERROR(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Internal bus error")),
		SELF_CHECK_FAILURE(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Self check failure")),
		ABNORMAL_SECURITY_FUNCTION(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Abnormal security function")),
		INSULATION_FAULT(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Insulation fault")),
		EMERGENCY_STOP_FAILURE(Doc.of(Level.FAULT) //
				.accessMode(READ_ONLY) //
				.text("Emergency stop failure")),
		NUMBER_OF_MODULES_IN_SERIES_PER_PILE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Number of modules in series per pile")),
		NUMBER_OF_CELLS_IN_SERIES_PER_PILE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Number of cells in series per pile")),
		CHARGE_FORBIDDEN_MARK(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Charge forbidden mark (1=yes, 0=no)")),
		DISCHARGE_FORBIDDEN_MARK(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY) //
				.text("Discharge forbidden mark (1=yes, 0=no)")),
		INSULATION_RESISTANCE(Doc.of(INTEGER) //
				.unit(KILOOHM) //
				.accessMode(READ_ONLY) //
				.text("Insulation resistance kOhm")),
		INSULATION_RESISTANCE_ERROR_LEVEL(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.text("Insulation resistance error level 0=OK, 1=Level 1, 2=Level 2")),

		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current state of state machine")),
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")) //
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
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the current system status.
	 * 
	 * @return a Status enum containing the current system status
	 */
	public default Status getSystemBasicStatus() {
		return this.getSystemBasicStatusChannel().value().asEnum();
	}

	/**
	 * Get the basic status channel.
	 * 
	 * @return The BASIC_STATUS channel
	 */
	public default Channel<Status> getSystemBasicStatusChannel() {
		return this.channel(ChannelId.BASIC_STATUS);
	}

	/**
	 * Awake/sleep channel.
	 * 
	 * @return Channel
	 *
	 */
	public default Channel<Integer> getWakeSleepChannel() {
		return this.channel(ChannelId.SLEEP_WAKE_CHANNEL);
	}

}