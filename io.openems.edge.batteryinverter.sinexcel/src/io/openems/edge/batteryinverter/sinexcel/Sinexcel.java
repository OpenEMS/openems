package io.openems.edge.batteryinverter.sinexcel;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.sinexcel.enums.ActivePowerControlMode;
import io.openems.edge.batteryinverter.sinexcel.enums.Baudrate;
import io.openems.edge.batteryinverter.sinexcel.enums.BlackStartMode;
import io.openems.edge.batteryinverter.sinexcel.enums.CpuType;
import io.openems.edge.batteryinverter.sinexcel.enums.DcVoltageLevel;
import io.openems.edge.batteryinverter.sinexcel.enums.Epo;
import io.openems.edge.batteryinverter.sinexcel.enums.FrequencyVariationRate;
import io.openems.edge.batteryinverter.sinexcel.enums.InterfaceType;
import io.openems.edge.batteryinverter.sinexcel.enums.ModulePowerLevel;
import io.openems.edge.batteryinverter.sinexcel.enums.PhaseAngleAbrupt;
import io.openems.edge.batteryinverter.sinexcel.enums.PowerRisingMode;
import io.openems.edge.batteryinverter.sinexcel.enums.ProtocolSelection;
import io.openems.edge.batteryinverter.sinexcel.enums.ReactivePowerControlMode;
import io.openems.edge.batteryinverter.sinexcel.enums.SinexcelGridMode;
import io.openems.edge.batteryinverter.sinexcel.enums.SinglePhaseMode;
import io.openems.edge.batteryinverter.sinexcel.enums.StartMode;
import io.openems.edge.batteryinverter.sinexcel.enums.Switch;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;

public interface Sinexcel extends OffGridBatteryInverter, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		OpenemsComponent, StartStoppable, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		CHARGE_MAX_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE)), //
		DISCHARGE_MAX_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE)), //
		CHARGE_MAX_CURRENT_READ(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE)), //
		DISCHARGE_MAX_CURRENT_READ(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE)), //
		CHARGE_MAX_VOLTAGE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)), //
		DISCHARGE_MIN_VOLTAGE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)), //

		ACTIVE_DISCHARGE_ENERGY_VALUE_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),
		ACTIVE_DISCHARGE_ENERGY_VALUE_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),
		ACTIVE_CHARGE_ENERGY_VALUE_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),
		ACTIVE_CHARGE_ENERGY_VALUE_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),

		// when implementing Lithium-ion batteries, these two registers MUST be set to
		// the same
		TOPPING_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)),
		// when implementing Lithium-ion batteries, these two registers MUST be set to
		// the same
		FLOAT_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)), //

		MANUFACTURER_AND_MODEL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_ONLY)), //
		FAULT_STATUS(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALERT_STATUS(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_INVERTER_STATE(new BooleanDoc() //
				.debounce(5, Debounce.FALSE_VALUES_IN_A_ROW_TO_SET_FALSE) //
				.<Sinexcel>onChannelChange((self, value) -> self._setInverterState(value.get()))),

		INVERTER_GRID_MODE(new BooleanDoc() //
				.debounce(5, Debounce.FALSE_VALUES_IN_A_ROW_TO_SET_FALSE) //
				.text("On Grid") //
				.<Sinexcel>onChannelChange((self, value) -> {
					final GridMode gridMode;
					if (!value.isDefined()) {
						gridMode = GridMode.UNDEFINED;
					} else if (value.get()) {
						gridMode = GridMode.ON_GRID;
					} else {
						gridMode = GridMode.OFF_GRID;
					}
					self._setGridMode(gridMode);
				})),

		ISLAND_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DERATING_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALLOW_GRID_CONNECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		STANDBY_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OBTAIN_FAULT_RECORD_FLAG(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		WRITE_POWER_GENERATION_INTO_EEPROM(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		INITIALIZE_DSP_PARAMETERS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		MASTER_SLAVE_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_OVER_FREQUENCY_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_UNDER_FREQUENCY_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_VOLTAGE_UNBALANCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_PHASE_REVERSE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		INVERTER_ISLAND(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		ON_GRID_OFF_GRID_SWITCH_OVER_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OUTPUT_GROUND_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_PHASE_LOCK_FAILED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		INTERNAL_AIR_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_CONNECTED_CONDITION_TIME_OUT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		MODULE_RENUMBER_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		// Monitor parallel use
		CANB_COMMUNICATION_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		POWER_FREQUENCY_SYNCHRONIZATION_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		CARRIER_SYNCHRONIZATION_FALURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EPO_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		MONITOR_PARAMETER_MISMATCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DSP_VERSION_ABNORMAL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		CPLD_VERSION_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		HARDWARE_VERSION_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		// Monitor to DSP
		CANA_COMMUNICATION_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AUXILARY_POWER_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		FAN_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_LOW_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_VOLTAGE_UNBALANCED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_RELAY_SHORT_CIRCUIT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OUTPUT_VOLTAGE_ABNORMAL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OUTPUT_CURRENT_UNBALANCED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OVER_TEMPERATURE_OF_HEAT_SINK(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		OUTPUT_OVER_LOAD_TOT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_CONTINUE_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_SOFT_START_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		INVERTER_START_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_RELAY_IS_OPEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		U2_BOARD_COMMUNICATION_IS_ABNORMAL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		AC_DC_COMPONENT_EXCESS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		MASTER_SLAVE_SAMPLING_ABNORMALITY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		PARAMETER_SETTING_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		LOW_OFF_GRID_ENERGY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		N_LINE_IS_NOT_CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		STANDBY_BUS_HEIGHT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		SINGLE_PHASE_WIRING_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EXCESSIVE_GRID_FREQUENCY_CHANGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		ABRUPT_PHASE_ANGLE_FAULT_OF_POWER_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_CONNECTION_PARAMETER_CONFLICT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EE_READING_ERROR_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EE_READING_ERROR_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_READING_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		INVERTER_OVER_LOAD(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_PARAMETER_SETTING_ERROR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		SLAVE_LOST_ALARM(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_DISCHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_FULLY_CHARGED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_EMPTY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_FAULT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_ALERT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_INPUT_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_INPUT_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_ALERT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_SOFT_START_FAILED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_RELAY_SHORT_CIRCUIT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_RELAY_SHORT_OPEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_POWEROVER_LOAD(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_POWER_OVER_LOAD(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_BUS_STARTING_FAILED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_QUICK_CHECK_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_OC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L1-L2 RMS voltage
		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		// AC L2-L3 RMS voltage
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		// AC L3-L1 RMS voltage
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		// AC L1 RMS current
		GRID_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		// AC L2 RMS current
		GRID_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		// AC L3 RMS current
		GRID_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		// AC frequency
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIHERTZ)), //
		// AC L1 Active Power
		ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		// AC L2 Active Power
		ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		// AC L3 Active Power
		ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		// AC L1 Reactive Power
		REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		// AC L2 Reactive Power
		REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		// AC L3 Reactive Power
		REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		// AC L1 Apparent Power
		APPERENT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)), //
		// AC L2 Apparent Power
		APPERENT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)), //
		// AC L3 Apparent Power
		APPERENT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)), //
		// AC L1 Power Factor
		COS_PHI_L1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L2 Power Factor
		COS_PHI_L2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L3 Power Factor
		COS_PHI_L3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC Apperent Power
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)), //
		// AC Power Factor
		COS_PHI(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //

		// Temperature of DC heat sink
		TEMPERATURE_OF_AC_HEAT_SINK(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.DEGREE_CELSIUS)), //
		// BUS+ side voltage
		DC_VOLTAGE_POSITIVE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		// BUS- side voltage
		DC_VOLTAGE_NEGATIVE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		// Target off-grid voltage bias
		SET_OFF_GRID_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.MILLIVOLT)), //
		// Target off-grid frequency bias
		SET_OFF_GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.MILLIHERTZ)), //
		DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)), //
		DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		TEMPERATURE_OF_DC_DC_HEAT_SINK(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.DEGREE_CELSIUS)), //
		DC_RELAY_REAR_END_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT)), //
		CHARGE_MAX_CURRENT_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		DISCHARGE_MAX_CURRENT_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIAMPERE)), //
		IP_ADDRESS_BLOCK_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		IP_ADDRESS_BLOCK_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		IP_ADDRESS_BLOCK_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		IP_ADDRESS_BLOCK_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		NETMASK_BLOCK_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		NETMASK_BLOCK_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		NETMASK_BLOCK_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		NETMASK_BLOCK_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GATEWAY_IP_BLOCK_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GATEWAY_IP_BLOCK_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GATEWAY_IP_BLOCK_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GATEWAY_IP_BLOCK_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MAC(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_UNIT_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BAUDRATE(Doc.of(Baudrate.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// default 1
		INTERFACE_TYPE(Doc.of(InterfaceType.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Takes effect after hard reset,0-modbus
		COMMUNICATION_PROTOCOL_SELECTION(Doc.of(ProtocolSelection.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Prevent unexpected results of communication failures.when EMS timeout
		// enabled, watchdog will work and even reading will feed the watchdog
		EMS_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Enable ONLY when remote Emergency Stop Button is needed
		EPO_ENABLE(Doc.of(Epo.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Enable ONLY when remote BMS-inverter connection is needed
		BMS_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Takes effect after hard reset,0-modbus
		BMS_PROTOCOL_SELECTION(Doc.of(ProtocolSelection.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_GRID_MODE(Doc.of(SinexcelGridMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		BUZZER_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESTORE_FACTORY_SETTING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// To Start operation, only 1 will be accepted. Reading back value makes no
		// sense
		START_INVERTER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// To Stop operation, only 1 will be accepted. Reading back value makes no sense
		STOP_INVERTER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// clear failure flag,when fault occurs, the system will stop and indicates
		// fault.starting is invalid until the fault source is actually removed and this
		// register is written 1. Reading back value makes no sense
		CLEAR_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		// set the module to on grid mode. Reading back value makes no sense
		SET_ON_GRID_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// set the module to off grid mode. Reading back value makes no sense
		SET_OFF_GRID_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// set the module to standby mode,setpoint 0:IGBT switching ,setpoint 1:no IGBT
		// switching,low consumption.let the inverter to halt the IGBT switching, to
		// save the power consumption, but all relays are still closed.
		// Reading back value makes no sense
		SET_STANDBY_COMMAND(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_SOFT_START(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESET_INSTRUCTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_STOP(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_LEVEL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_LEVEL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		INVERTER_WIRING_TOPOLOGY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		SWITCHING_DEVICE_ACCESS_SETTING(Doc.of(Switch.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODULE_POWER_LEVEL(Doc.of(ModulePowerLevel.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		DC_VOLTAGE_LEVEL(Doc.of(DcVoltageLevel.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		CPU_TYPE(Doc.of(CpuType.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		OFF_GRID_AND_PARALLEL_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_DC_SOFT_START_EXTERNAL_CONTROL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_OVER_VOLTAGE_PROTECTION_AMPLITUDE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_VOLTAGE_TRIP_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_VOLTAGE_TRIP_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_VOLTAGE_TRIP_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_LEVEL_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_LEVEL_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_VOLTAGE_TRIP_TIME_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_FREQUENCY_TRIP_LEVEL_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_FREQUENCY_TRIP_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_FREQUENCY_TRIP_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_OVER_FREQUENCY_TRIP_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_FREQUENCY_TRIP_LEVEL_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_FREQUENCY_TRIP_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_FREQUENCY_TRIP_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AC_UNDER_FREQUENCY_TRIP_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RECONNECT_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Anti-islanding is a CPUC RULE 21/HECO RULE14H/IEEE1547-requested function to
		// make sure the inverter disconnect from the grid in case of blackout.
		// This is to prevent the formation of an unintended island. The inverter design
		// shall comply with the requirements of IEEE Std 1547 and UL 1741 standards (or
		// latest versions) and be certified to have anti-islanding protection such that
		// the synchronous inverter will automatically disconnect upon a utility system
		// interruption
		ANTI_ISLANDING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Frequency and Voltage Ride-Through.
		// The ability to withstand voltage or frequency excursions outside defined
		// limits without tripping or malfunctioning
		FREQUENCY_VOLTAGE_RIDE_THROUGH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid-tied mode only, voltage and frequency setpoint the only setpoints
		REACTIVE_POWER_CONTROL_MODE(Doc.of(ReactivePowerControlMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// To define how the power changes
		POWER_RISING_MODE(Doc.of(PowerRisingMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid-tied mode only, Volt/Watt control & Freq/Watt control means active power
		// will be regulated by grid voltage/frequency following a curve/ramp rate given
		// by HECO or CPUC or other local utility authority codes
		ACTIVE_POWER_CONTROL_MODE(Doc.of(ActivePowerControlMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid-tied mode only, voltage and frequency setpoint the only setpoints
		GRID_VOLTAGE_ASYMMETRIC_DETECTON(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid-tied mode only
		CONTINUOUS_OVERVOLTAGE_DETECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid-tied mode only,detect whether the grid is on-service at powered-up.
		GRID_EXISTENCE_DETECTION_ON(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
		// Grid-tied mode only
		NEUTRAL_FLOATING_DETECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// if disabled, the inverter will start the AC voltage, then close the relay. In
		// some off-grid cases, such as there are inductive loads or transformer, the
		// in rush exciting current will trip the inverter. Enabling this register, the
		// inverter will close the relay first try to limit the current and start the
		// voltage slowly
		OFF_GRID_BLACKSTART_MODE(Doc.of(BlackStartMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Grid tied mode only. take effect after power off.
		GRID_CODE_SELECTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_CONNECTED_ACTIVE_CAPACITY_LIMITATION_FUNCTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_ACTIVE_POWER_CAPACITY_SETTING(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Single mode enable and select
		SINGLE_PHASE_MODE_SELECTION(Doc.of(SinglePhaseMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Overvoltage drop active enable (only for EN50549 certification)
		OVER_VOLTAGE_DROP_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		// 0-Manual start,1-Auto start,default:0
		START_UP_MODE(Doc.of(StartMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCAL_ID_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// when implementing Lithium-ion batteries,this register MUST be set to 0
		CURRENT_FROM_TOPPING_CHARGING_TO_FLOAT_CHARGING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_VOLTAGE_PROTECTION_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)), //
		LEAKAGE_CURRENT_DC_COMPONENT_DETECTOR(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESUME_AND_LIMIT_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESTORE_LOWER_FREQUENCY_OF_GRID_CONNECTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_REACTIVE_REFERENCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// ratio * rated voltage,Available only when reactive power regulation mode is
		// set to Volt/Var(53626).
		VOLTAGE_AND_REACTIVE_POWER_ADJUSTMENT_POINT_V1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_AND_REACTIVE_POWER_ADJUSTMENT_POINT_V2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_AND_REACTIVE_POWER_ADJUSTMENT_POINT_V3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_AND_REACTIVE_POWER_ADJUSTMENT_POINT_V4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// refer to HECO RULE 14, keep default value if no aware of it
		MAX_CAPACITIVE_REACTIVE_REGULATION_Q1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		INITIAL_CAPACITIVE_REACTIVE_REGULATION_Q2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		INITIAL_INDUCTIVE_REACTIVE_REGULATION_Q3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MAX_INDUCTIVE_REACTIVE_REGULATION_Q4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLTAGE_AND_REACTIVE_RESPONSE_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		REACTIVE_FIRST_ORDER_RESPONSE_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// ratio * rated voltage, Available only when active power regulation
		// mode(53636) is set to Volt/Watt and operating in discharge mode, follow the
		// FVRT table given by HECO or CPUC or other local utility authority codes.
		INITIAL_VOLTAGE_V_START(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		END_VOLTAGE_V_STOP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		INITIAL_POWER_P_START(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		END_POWER_P_STOP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RETURN_TO_SERVICE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		VOLT_WATT_RESPONSE_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Available only when active power regulation mode(53636) is set to Freq/Watt
		// and operating in discharge mode. When the actual frequency is above the
		// point, the active power will be regulated(lowered) with the ramp rate. In
		// Australia, this register shall be fixed to 0.25Hz
		START_OF_FREQUENY_DROP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// The ramp rate is defined as multiple of set active power per hertz that above
		// the above the Freq/Watt regulation point. Available only when active power
		// regulation mode(53636) is set to Freq/Watt and operating in discharge mode.
		// Example: Rated frequency is 60Hz, the target active power is set to 10kW,
		// Freq/Watt regulation point is set to 2Hz, ramp rate is set as 0.5, If the
		// actual frequency reaches 63Hz, the output active power will be
		// 10kW-(63Hz-62Hz) x 0.5*(10kW/Hz) = 5kW
		SLOPE_OF_FREQUENCY_DROP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// AS4777 only, bias Bias from rated frequency
		FREQUENCY_WATT_F_STOP_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_WATT_F_STOP_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// %Vrated,AS4777 only, ratio
		VOLT_WATT_V_START_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// SS takes effect when the inverter starts, or when the inverter is on "grid
		// reconnection" after the trip caused by FVRT timeout
		SOFT_START_RAMP_RATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// RR takes effect in Volt-Watt or Freq-Watt mode when the grid is back to
		// normal state and the inverter trying to go back to normal output.
		// In other words, as long as the inverter does not trip, and the inverter had
		// derated the output power, RR takes effect when the inverter tries to go back
		// to normal output. Available only when Power rising mode is set to ramp
		// mode(53626).If the value is 2.000, which means within 0.5 seconds the system
		// can runs to full power output.
		POWER_RAMP_RATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Available only when Constant PF is enabled,Negative inductive, positive
		// capacitive
		POWER_FACTOR_SETTING(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Available only when active power regulation mode(53636) is set to Freq/Watt
		// and operating in discharge mode. When the actual frequency is above the
		// point, the active power will be regulated(lowered) with the ramp rate
		POWER_FACTOR_P1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_P2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_P3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_P4(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// +(lagging), -(leading),
		POWER_FACTOR_CURVE_MODE_P1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_CURVE_MODE_P2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_CURVE_MODE_P3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FACTOR_CURVE_MODE_P4(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// %Vrated
		CONTINUOS_OVER_VOLTAGE_TRIP_THRESHOLD(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_VARIATION_RATE_TRIP_THRESHOLD(Doc.of(FrequencyVariationRate.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		PHASE_ANGLE_ABRUPT_TRIP_THRESHOLD(Doc.of(PhaseAngleAbrupt.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Available only when Power rising mode is set to ramp mode . once tripped
		// after FVRT timeout, the inverter can reconnect to the grid when frequency or
		// voltage is back to the thresholds defined as "grid is back to service". In
		// HECO14 /CPUC 21, this register is unnecessary to
		GRID_RECONNECTION_VOLTAGE_UPPER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_RECONNECTION_VOLTAGE_LOWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Available only when Power rising mode is set to ramp mode once tripped after
		// FVRT timeout, the inverter can reconnect to the grid when frequency or
		// voltage is back to the thresholds defined as "grid is back to service"
		GRID_RECONNECTION_FREQUENCY_UPPER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// Bias from rated frequency
		GRID_RECONNECTION_FREQUENCY_LOWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOW_FREQUENCY_RAMP_RATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		// TODO Values check Meter options !!!!!!!!!!!!!!!!!!
		METER_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE).unit(Unit.WATT)), //

		GRID_VOLTAGE_CALIBRATION_L1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLTAGE_CALIBRATION_L2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLTAGE_CALIBRATION_L3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INVERTER_VOLTAGE_CALIBRATION_L1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INVERTER_VOLTAGE_CALIBRATION_L2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INVERTER_VOLTAGE_CALIBRATION_L3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L1_PARAMETERS_1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L2_PARAMETERS_1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L3_PARAMETERS_1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L1_PARAMETERS_2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L2_PARAMETERS_2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		INDUCTOR_CURRENT_CALIBRATION_L3_PARAMETERS_2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		OUTPUT_CURRENT_CALIBRATION_L1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		OUTPUT_CURRENT_CALIBRATION_L2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		OUTPUT_CURRENT_CALIBRATION_L3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		POSITIVE_BUS_VOLTAGE_CALIBRATION(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		NEGATIVE_BUS_VOLTAGE_CALIBRATION(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		DC_VOLTAGE_CALIBRATION(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		DC_CURRENT_CALIBRATION(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		DC_INDUCTOR_CURRENT_CALIBRATION(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_WRITE)), //
		TIME_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		PASSWORD(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)) //
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
	 * Gets the Channel for {@link ChannelId#SET_ON_GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetOnGridModeChannel() {
		return this.channel(ChannelId.SET_ON_GRID_MODE);
	}

	/**
	 * Gets the Set-On-Grid-Mode. See {@link ChannelId#SET_ON_GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSetOnGridMode() {
		return this.getSetOnGridModeChannel().value();
	}

	/**
	 * Sets a the On-Grid-Mode. See {@link ChannelId#SET_ON_GRID_MODE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOnGridMode(Boolean value) throws OpenemsNamedException {
		this.getSetOnGridModeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OFF_GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetOffGridModeChannel() {
		return this.channel(ChannelId.SET_OFF_GRID_MODE);
	}

	/**
	 * Gets the Set-Off-Grid-Mode. See {@link ChannelId#SET_OFF_GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSetOffGridMode() {
		return this.getSetOffGridModeChannel().value();
	}

	/**
	 * Sets a the Off-Grid-Mode. See {@link ChannelId#SET_OFF_GRID_MODE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOffGridMode(Boolean value) throws OpenemsNamedException {
		this.getSetOffGridModeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#START_INVERTER}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getStartInverterChannel() {
		return this.channel(ChannelId.START_INVERTER);
	}

	/**
	 * Sends a START command to the inverter. See {@link ChannelId#START_INVERTER}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void setStartInverter() throws OpenemsNamedException {
		this.getStartInverterChannel().setNextWriteValue(true); // true = START
	}

	/**
	 * Gets the Channel for {@link ChannelId#STOP_INVERTER}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getStopInverterChannel() {
		return this.channel(ChannelId.STOP_INVERTER);
	}

	/**
	 * Sends a STOP command to the inverter. See {@link ChannelId#STOP_INVERTER}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void setStopInverter() throws OpenemsNamedException {
		this.getStopInverterChannel().setNextWriteValue(true); // true = STOP
	}

	/**
	 * Gets the Channel for {@link ChannelId#CLEAR_FAILURE}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getClearFailureChannel() {
		return this.channel(ChannelId.CLEAR_FAILURE);
	}

	/**
	 * Clear inverter failures. See {@link ChannelId#CLEAR_FAILURE}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void setClearFailure() throws OpenemsNamedException {
		this.getClearFailureChannel().setNextWriteValue(true);
	}
}
