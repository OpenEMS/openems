package io.openems.edge.battery.fenecon.commercial;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface FeneconCommercialBattery extends Battery, StartStoppable, OpenemsComponent {

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for {@link ChannelId#RUNNING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getMasterStartedChannel() {
		return this.channel(ChannelId.RUNNING);
	}

	/**
	 * Gets the PowerOn, see {@link ChannelId#RUNNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMasterStarted() {
		return this.getMasterStartedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUNNING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMasterBmsStarted(Boolean value) {
		this.getMasterStartedChannel().setNextValue(value);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNIT_ID(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.<FeneconCommercialBatteryImpl>onValueChange(FeneconCommercialBatteryImpl::updateSoc)),
		UNIT_NUMBER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		SUBMASTER_MAP(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		BAUDRATE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_SOC(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.<FeneconCommercialBatteryImpl>onValueChange(FeneconCommercialBatteryImpl::updateSoc)),
		BATTERY_CHARGE_MAX_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.<FeneconCommercialBatteryImpl>onValueChange(FeneconCommercialBatteryImpl::updateSoc)),
		BATTERY_DISCHARGE_MAX_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.<FeneconCommercialBatteryImpl>onValueChange(FeneconCommercialBatteryImpl::updateSoc)),
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.WARNING) //
				.text("Running the Logic failed")), //
		MAX_START_ATTEMPTS(Doc.of(Level.WARNING) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.WARNING) //
				.text("The maximum number of stop attempts failed")), //
		NUMBER_OF_TOWERS(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Number of modules per tower") //
				.<FeneconCommercialBatteryImpl>onValueChange(
						FeneconCommercialBatteryImpl::updateNumberOfTowersAndModulesAndCells)),
		NUMBER_OF_MODULES_PER_TOWER(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Number of modules per tower") //
				.<FeneconCommercialBatteryImpl>onValueChange(
						FeneconCommercialBatteryImpl::updateNumberOfTowersAndModulesAndCells)),
		NUMBER_OF_CELLS_PER_MODULE(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Number of cells per module") //
				.<FeneconCommercialBatteryImpl>onValueChange(
						FeneconCommercialBatteryImpl::updateNumberOfTowersAndModulesAndCells)),

		// Versions
		MASTER_MCU_HARDWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MASTER_MCU_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		SLAVE_MCU_HARDWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		SLAVE_MCU_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		// Master BMS RO
		ONLINE_TOWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		RUNNING_TOWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		NOMINAL_CAPACITY(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE_HOURS)), //
		TOTAL_CHARGE_CAPACITY_AMPERE_HOURS(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE_HOURS)), //
		TOTAL_DISCHARGE_CAPACITY_AMPERE_HOURS(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE_HOURS)), //
		MAX_SOC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.PERCENT)), //
		MAX_SOC_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.PERCENT)), //
		MIN_SOC_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MAX_CELL_VOLTAGE_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MAX_CELL_VOLTAGE_CELL_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MIN_CELL_VOLTAGE_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MIN_CELL_VOLTAGE_CELL_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)),
		MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MAX_TEMPERATURE_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MAX_TEMPERATURE_POSITION(Doc.of(TemperaturePosition.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAX_TEMPERATURE_MODULE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MIN_TEMPERATURE_POSITION(Doc.of(TemperaturePosition.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		MIN_TEMPERATURE_MODULE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		MIN_TEMPERATURE_TOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		INSULATION_RESISTANCE_AT_POSITIVE_POLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.OHM)), //
		INSULATION_RESISTANCE_AT_NEGATIVE_POLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.OHM)), //
		TOTAL_CHARGE_CAPACITY_WATT_HOURS(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT_HOURS)), //
		TOTAL_DISCHARGE_CAPACITY_WATT_HOURS(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT_HOURS)), //

		// 1.2 SysProtectMessage
		SYSTEM_FAULT_COUNTERS(Doc.of(OpenemsType.INTEGER) //
				.text("Fault counters")), //
		FAULT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Fault Status")), //
		POWER_ON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power On")), //
		LOW_SELF_CONSUMPTION_STATUS(Doc.of(Level.WARNING) //
				.text("Low self consumption status")), //
		FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Fault")), //
		RUNNING(Doc.of(OpenemsType.BOOLEAN) //
				.text("Running")), //
		EXTERNAL_COMMUNICATION_ONLY_UNDER_STANDBY(Doc.of(Level.WARNING) //
				.text("External communication only under standby")), //
		MAIN_SWITCH_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Main switch status")), //
		BATTERY_ONLINE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery online")), //
		PCS_ONLINE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs online")), //
		UPS_ONLINE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ups online")), //
		STS_ONLINE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Sts online")), //
		BATTERY_18650_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery 18650 Low")), //
		MASTER_CPU_INITIALIZE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Master cpu initilalize")), //
		SLAVE_CPU_INITIALIZE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Slave cpu initilalize")), //
		BATTERY_SYSTEM_INITIALIZE_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery system initilalize active")), //
		PCS_INITIALIZE_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs initilalize active")), //
		UPS_INITIALIZE_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ups initilalize active")), //
		MASTER_CPU_INITIALIZE_FINISH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Master cpu initilalize finish")), //
		SLAVE_CPU_INITIALIZE_FINISH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Slave cpu initilalize finish")), //
		BATTERY_SYSTEM_INITIALIZE_FINISH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battetry system initilalize finish")), //
		PCS_INITIALIZE_FINISH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs initilalize finish")), //
		UPS_INITIALIZE_FINISH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ups initilalize finish")), //
		MASTER_CPU_INITIALIZE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.text("Master cpu initilalize fail")), //
		SLAVE_CPU_INITIALIZE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.text("Slave cpu initilalize fail")), //
		BATTERY_SYSTEM_INITIALIZE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery system initilalize fail")), //
		PCS_INITIALIZE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs initilalize fail")), //
		UPS_INITIALIZE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ups initilalize fail")), //
		DRY_CONTACT_FAIL(Doc.of(Level.WARNING) //
				.text("Dry contact fail")), //
		POWER_SUPPLY_24V_FAIL(Doc.of(Level.WARNING) //
				.text("Power supply 24V fail")), //
		EEPROM2_FAULT(Doc.of(Level.WARNING) //
				.text("Eeprom 2 fault")), //
		BATTERY_18650_FAULT(Doc.of(Level.WARNING) //
				.text("Battery 18650 fault")), //
		BATTERY_SYSTEM_FAULT(Doc.of(Level.WARNING) //
				.text("Batery System fault")), //
		NO_BATTERY(Doc.of(Level.WARNING) //
				.text("No battery")), //
		PCS_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs fault")), //
		NO_PCS(Doc.of(OpenemsType.BOOLEAN) //
				.text("No pcs")), //
		UPS_FAULT(Doc.of(Level.WARNING) //
				.text("Ups fault")), //
		NO_UPS(Doc.of(Level.WARNING) //
				.text("No ups")), //
		INSULATION_RESISTANCE_DETECTION_FAULT(Doc.of(Level.WARNING) //
				.text("Insulation resistance detection fault")), //
		SLAVE_MCU_FAULT(Doc.of(Level.WARNING) //
				.text("Slave mcu fault")), //
		SYSTEM_TEMPERATURE_FAULT(Doc.of(Level.WARNING) //
				.text("System temperature fault")), //
		PCS_STOP(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pcs stop")), //
		METER_FAULT(Doc.of(Level.WARNING) //
				.text("Meter fault")), //
		BATTERY_TOWERS_TEMPERATURE_SENSORS_FAULT(Doc.of(Level.WARNING) //
				.text("Battery towers temperature sensor fault")), //
		SYSTEM_TEMPERATURE_SENSORS_FAULT(Doc.of(Level.WARNING) //
				.text("System temperature sensors fault")), //
		SYSTEM_OVER_TEMPERATURE_FAULT(Doc.of(Level.WARNING) //
				.text("System over temperature fault")), //
		SYSTEM_LOW_TEMPERATURE_FAULT(Doc.of(Level.WARNING) //
				.text("System ow temperature fault")), //
		STS_FAULT(Doc.of(Level.WARNING) //
				.text("Sts fault")), //
		PCS_OVER_TEMPERATURE_FAULT(Doc.of(Level.WARNING) //
				.text("Pcs over temperature fault")), //
		EEPROM_FAULT(Doc.of(Level.WARNING) //
				.text("Eeprom fault")), //
		FLASH_FAULT(Doc.of(Level.WARNING) //
				.text("Flash fault")), //
		EMS_FAULT(Doc.of(Level.WARNING) //
				.text("Ems fault")), //
		SD_FAULT(Doc.of(Level.WARNING) //
				.text("Sd fault")), //
		BATTERY_18650_WARNING(Doc.of(Level.WARNING) //
				.text("Battery 18650 warning")), //
		MASTER_BATTERY_WARNING(Doc.of(Level.WARNING) //
				.text("Master battery warning")), //
		PCS_WARNING(Doc.of(Level.WARNING) //
				.text("Pcs warning")), //
		UPS_WARNING(Doc.of(Level.WARNING) //
				.text("Ups warning")), //
		SLAVE_MCU_WARNING(Doc.of(Level.WARNING) //
				.text("Slave mcu warning")), //
		SYSTEM_TOO_MUCH_OVER_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("System too much over temperature warning")), //
		SYSTEM_OVER_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("System over tmeperature warning")), //
		SYSTEM_TOO_MUCH_LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("Syste, too much low temperature warning")), //
		SYSTEM_LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("System low temperature warning")), //
		FAN_FAULT(Doc.of(Level.WARNING) //
				.text("Fan fault")), //
		// Bit After Fan fault was not described well.
		BATTERY_TOWERS_TEMPERATURE_SENSORS_WARNING(Doc.of(Level.WARNING) //
				.text("Battery towers temperature sensors warning")), //
		SYSTEM_TEMPERATURE_SENSORS_WARNING(Doc.of(Level.WARNING) //
				.text("System temperature sensors warning")), //
		STS_WARNING(Doc.of(Level.WARNING) //
				.text("Sts warning")), //
		PCS_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("Pcb temperature warning")), //
		PCS_OVER_TEMPERATURE(Doc.of(Level.WARNING) //
				.text("Pcs over Temperature")), //
		OVER_TEMPERATURE_STOP_PCS(Doc.of(Level.WARNING) //
				.text("Over temperature stop PCS")), //
		LOW_TEMPERATURE_STOP_PCS(Doc.of(Level.WARNING) //
				.text("Low temperature stop PCS")), //
		OVER_CURRENT_STOP_CHARGING(Doc.of(Level.WARNING) //
				.text("Over current stop charging")), //
		OVER_CURRENT_STOP_DISCHARGING(Doc.of(Level.WARNING) //
				.text("Over current stop discharging")), //
		OVER_TEMPERATURE_STOP_CHARGING(Doc.of(Level.WARNING) //
				.text("Over temperature stop charging")), //
		LOW_TEMPERATURE_STOP_DISCHARGING(Doc.of(Level.WARNING) //
				.text("Low temperature stop discharging")), //
		VOLTAGE_DIFFERENCE_HIGH_STOP_PCS(Doc.of(Level.WARNING) //
				.text("Voltage difference high stop PCS")), //
		POWER_HIGH_STOP_PCS(Doc.of(Level.WARNING) //
				.text("Power high stop PCS")), //
		VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Voltage high")), //
		VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Voltage low")), //
		TEMPERATURE_HIGH(Doc.of(Level.WARNING) //
				.text("Temperature high")), //
		TEMPERATURE_LOW(Doc.of(Level.WARNING) //
				.text("Temperature Low")), //

		// Master BMS RO continue
		INSULATION_RESISTANCE_DETECTION_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		RELAY_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		BATTERY_MAX_CELL_VOLT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		BATTERY_MIN_CELL_VOLT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		BATTERY_NOMINAL_POWER(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		BATTERY_AVAILABLE_POWER(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		START_CHARGE_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT)), //
		START_DISCHARGE_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT)), //
		CHARGE_MAX_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		DISCHARGE_MAX_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		BATTERY_NOMINAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE)), //
		BATTERY_AVAILABLE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE)), //
		SOC_FOR_INVERTER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.PERCENT)), //
		FORCE_TO_CHARGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		CHARGE_READY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		DISCHARGE_READY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.NONE)), //
		// Master BMS RW
		HEART_BEAT(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.NONE)), //
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
}
