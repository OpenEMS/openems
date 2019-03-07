package io.openems.edge.sma;

import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.sma.enums.AbsorptionPhaseActive;
import io.openems.edge.sma.enums.AcknowledgeGeneratorErrors;
import io.openems.edge.sma.enums.ActiveBatteryChargingMode;
import io.openems.edge.sma.enums.AutomaticFrequencySynchronization;
import io.openems.edge.sma.enums.AutomaticGeneratorStart;
import io.openems.edge.sma.enums.BMSOperatingMode;
import io.openems.edge.sma.enums.BatteryType;
import io.openems.edge.sma.enums.ConfigurationOfTheCosphiEndPoint;
import io.openems.edge.sma.enums.ConfigurationOfTheCosphiStartingPoint;
import io.openems.edge.sma.enums.ControlOfBatteryChargingViaCommunicationAvailable;
import io.openems.edge.sma.enums.DataTransferRateOfNetworkTerminalA;
import io.openems.edge.sma.enums.DuplexModeOfNetworkTerminalA;
import io.openems.edge.sma.enums.GeneratorStatus;
import io.openems.edge.sma.enums.GridCreatingGenerator;
import io.openems.edge.sma.enums.GridRequestViPowerSwitchOn;
import io.openems.edge.sma.enums.GridRequestViaChargeType;
import io.openems.edge.sma.enums.ManualControlOfNetworkConnection;
import io.openems.edge.sma.enums.ManualEqualizationCharge;
import io.openems.edge.sma.enums.ManualGeneratorStart;
import io.openems.edge.sma.enums.MemoryCardStatus;
import io.openems.edge.sma.enums.MeterSetting;
import io.openems.edge.sma.enums.MultifunctionRelayStatus;
import io.openems.edge.sma.enums.OperatingModeForActivePowerLimitation;
import io.openems.edge.sma.enums.OperatingModeOfActivePowerLimitationAtOverFrequency;
import io.openems.edge.sma.enums.PowerFeedbackToPublicGridAllowed;
import io.openems.edge.sma.enums.PowerSupplyStatus;
import io.openems.edge.sma.enums.PvMainsConnection;
import io.openems.edge.sma.enums.ReasonForGeneratorRequest;
import io.openems.edge.sma.enums.RepetitionCycleOfTheControlledInverter;
import io.openems.edge.sma.enums.RepetitionCycleOfTheTimeControlledGeneratorOperation;
import io.openems.edge.sma.enums.RiseInSelfConsumptionSwitchedOn;
import io.openems.edge.sma.enums.SetControlMode;
import io.openems.edge.sma.enums.SpeedWireConnectionStatusOfNetworkTerminalA;
import io.openems.edge.sma.enums.StatusBatteryApplicationArea;
import io.openems.edge.sma.enums.StatusDigitalInput;
import io.openems.edge.sma.enums.StatusOfUtilityGrid;
import io.openems.edge.sma.enums.SystemState;
import io.openems.edge.sma.enums.TimeControlledGeneratorOperation;
import io.openems.edge.sma.enums.TimeControlledInverterOperation;
import io.openems.edge.sma.enums.TypeOfACSubdistribution;

public enum SiChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		DEVICE_CLASS(new Doc()), //
		DEVICE_TYPE(new Doc()), //
		SERIAL_NUMBER(new Doc()), //
		SOFTWARE_PACKAGE(new Doc()), //
		WAITING_TIME_UNTIL_FEED_IN(new Doc().unit(Unit.SECONDS)), //
		MESSAGE(new Doc()), //
		SYSTEM_STATE(new Doc().options(SystemState.values())), //
		RECOMMENDED_ACTION(new Doc()), //
		FAULT_CORRECTION_MEASURE(new Doc()), //
		NUMBER_OF_EVENT_FOR_USER(new Doc()), //
		NUMBER_OF_EVENT_FOR_INSTALLER(new Doc()), //
		NUMBER_OF_EVENT_FOR_SERVICE(new Doc()), //
		NUMBER_OF_GENERATORS_STARTS(new Doc()), //
		AMP_HOURS_COUNTER_FOR_BATTERY_CHARGE(new Doc().unit(Unit.AMPERE_HOURS)), //
		AMP_HOURS_COUNTER_FOR_BATTERY_DISCHARGE(new Doc().unit(Unit.AMPERE_HOURS)), //
		METER_READING_CONSUMPTION_METER(new Doc().unit(Unit.WATT_HOURS)), //
		ENERGY_CONSUMED_FROM_GRID(new Doc().unit(Unit.WATT_HOURS)), //
		ENERGY_FED_INTO_GRID(new Doc().unit(Unit.WATT_HOURS)), //
		GRID_REFERENCE_COUNTER_READING(new Doc().unit(Unit.WATT_HOURS)), //
		GRID_FEED_IN_COUNTER_READING(new Doc().unit(Unit.WATT_HOURS)), //
		POWER_OUTAGE(new Doc().unit(Unit.SECONDS)), //
		RISE_IN_SELF_CONSUMPTION(new Doc().unit(Unit.WATT_HOURS)), //
		RISE_IN_SELF_CONSUMPTION_TODAY(new Doc().unit(Unit.WATT_HOURS)), //
		ABSORBED_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		RELEASED_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		NUMBER_OF_GRID_CONNECTIONS(new Doc()), //
		ACTIVE_POWER_L1(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWER_L2(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWER_L3(new Doc().unit(Unit.WATT)), //
		GRID_VOLTAGE_L1(new Doc().unit(Unit.VOLT)), //
		GRID_VOLTAGE_L2(new Doc().unit(Unit.VOLT)), //
		GRID_VOLTAGE_L3(new Doc().unit(Unit.VOLT)), //
		FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		REACTIVE_POWER_L1(new Doc().unit(Unit.VOLT_AMPERE)), //
		REACTIVE_POWER_L2(new Doc().unit(Unit.VOLT_AMPERE)), //
		REACTIVE_POWER_L3(new Doc().unit(Unit.VOLT_AMPERE)), //
		COSPHI_SET_POINT_READ(new Doc()), //
		CURRENT_BATTERY_CAPACITY(new Doc().unit(Unit.PERCENT)), //
		ACTIVE_BATTERY_CHARGING_MODE(new Doc()//
				.options(ActiveBatteryChargingMode.values())), //
		CURRENT_BATTERY_CHARGING_SET_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		NUMBER_OF_BATTERY_CHARGE_THROUGHPUTS(new Doc()), //
		BATTERY_MAINT_SOC(new Doc()), //
		LOAD_POWER(new Doc().unit(Unit.WATT)), //
		POWER_GRID_REFERENCE(new Doc().unit(Unit.WATT)), //
		POWER_GRID_FEED_IN(new Doc().unit(Unit.WATT)), //
		PV_POWER_GENERATED(new Doc().unit(Unit.WATT)), //
		CURRENT_SELF_CONSUMPTION(new Doc().unit(Unit.WATT)), //
		CURRENT_RISE_IN_SELF_CONSUMPTION(new Doc().unit(Unit.WATT)), //
		MULTIFUNCTION_RELAY_STATUS(new Doc()//
				.options(MultifunctionRelayStatus.values())), //
		POWER_SUPPLY_STATUS(new Doc()//
				.options(PowerSupplyStatus.values())), //
		REASON_FOR_GENERATOR_REQUEST(new Doc()//
				.options(ReasonForGeneratorRequest.values())), //
		PV_MAINS_CONNECTION(new Doc()//
				.options(PvMainsConnection.values())), //
		STATUS_OF_UTILITY_GRID(new Doc()//
				.options(StatusOfUtilityGrid.values())), //
		GRID_FREQ_OF_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.VOLT)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.AMPERE)), //
		GENERATOR_STATUS(new Doc()//
				.options(GeneratorStatus.values())), //
		DATA_TRANSFER_RATE_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(DataTransferRateOfNetworkTerminalA.values())), //
		DUPLEX_MODE_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(DuplexModeOfNetworkTerminalA.values())), //
		SPEED_WIRE_CONNECTION_STATUS_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(SpeedWireConnectionStatusOfNetworkTerminalA.values())), //
		GRID_CURRENT_L1(new Doc().unit(Unit.AMPERE)), //
		GRID_CURRENT_L2(new Doc().unit(Unit.AMPERE)), //
		GRID_CURRENT_L3(new Doc().unit(Unit.AMPERE)), //
		OUTPUT_OF_PHOTOVOLTAICS(new Doc()), //
		TOTAL_CURRENT_EXTERNAL_GRID_CONNECTION(new Doc().unit(Unit.AMPERE)), //
		FAULT_BATTERY_SOC(new Doc().unit(Unit.PERCENT)), //
		MAXIMUM_BATTERY_CURRENT_IN_CHARGE_DIRECTION(new Doc().unit(Unit.AMPERE)), //
		MAXIMUM_BATTERY_CURRENT_IN_DISCHARGE_DIRECTION(new Doc().unit(Unit.AMPERE)), //
		CHARGE_FACTOR_RATIO_OF_BATTERY_CHARGE_DISCHARGE(new Doc()), //
		OPERATING_TIME_OF_BATTERY_STATISTICS_COUNTER(new Doc().unit(Unit.SECONDS)), //
		LOWEST_MEASURED_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		HIGHEST_MEASURED_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		MAX_OCCURRED_BATTERY_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		REMAINING_TIME_UNTIL_FULL_CHARGE(new Doc().unit(Unit.SECONDS)), //
		REMAINING_TIME_UNTIL_EQUALIZATION_CHARGE(new Doc().unit(Unit.SECONDS)), //
		REMAINING_ABSORPTION_TIME(new Doc().unit(Unit.SECONDS)), //
		LOWER_DISCHARGE_LIMIT_FOR_SELF_CONSUMPTION_RANGE(new Doc().unit(Unit.PERCENT)), //
		TOTAL_OUTPUT_CURRENT_OF_SOLAR_CHARGER(new Doc().unit(Unit.AMPERE)), //
		REMAINING_MIN_OPERATING_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		OPERATING_STATUS_MASTER_L1(new Doc()), //
		STATUS_BATTERY_APPLICATION_AREA(new Doc()//
				.options(StatusBatteryApplicationArea.values())), //
		ABSORPTION_PHASE_ACTIVE(new Doc()//
				.options(AbsorptionPhaseActive.values())), //
		CONTROL_OF_BATTERY_CHARGING_VIA_COMMUNICATION_AVAILABLE(new Doc()//
				.options(ControlOfBatteryChargingViaCommunicationAvailable.values())), //
		TOTAL_ENERGY_PHOTOVOLTAICS(new Doc().unit(Unit.KILOWATT_HOURS)), //
		TOTAL_ENERGY_PHOTOVOLTAICS_CURRENT_DAY(new Doc().unit(Unit.WATT_HOURS)), //
		NUMBER_OF_EQALIZATION_CHARGES(new Doc().unit(Unit.KILOWATT_HOURS)), //
		NUMBER_OF_FULL_CHARGES(new Doc()), //
		RELATIVE_BATTERY_DISCHARGING_SINCE_THE_LAST_FULL_CHARGE(new Doc().unit(Unit.PERCENT)), //
		RELATIVE_BATTERY_DISCHARGING_SINCE_LAST_EQUALIZATION_CHARGE(new Doc().unit(Unit.PERCENT)), //
		OPERATING_TIME_ENERGY_COUNT(new Doc().unit(Unit.SECONDS)), //
		PHOTOVOLTAIC_ENERGY_IN_SOLAR_CHARGER(new Doc().unit(Unit.WATT_HOURS)), //
		BATTERY_CHARGING_SOC(new Doc().unit(Unit.WATT)), //
		BATTERY_DISCHARGING_SOC(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L1(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L2(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L3(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		STATUS_DIGITAL_INPUT(new Doc()//
				.options(StatusDigitalInput.values())), //
		RATED_BATTERY_CAPACITY(new Doc()), //
		MAX_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TYPE(new Doc()//
				.options(BatteryType.values())), //
		RATED_BATTERY_VOLTAGE(new Doc()), //
		BATTERY_BOOST_CHARGE_TIME(new Doc().unit(Unit.MINUTE)), //
		BATTERY_EQUALIZATION_CHARGE_TIME(new Doc().unit(Unit.HOUR)), //
		BATTERY_FULL_CHARGE_TIME(new Doc().unit(Unit.HOUR)), //
		MAX_BATTERY_CHARGING_CURRENT(new Doc().unit(Unit.AMPERE)), //
		RATED_GENERATOR_CURRENT(new Doc().unit(Unit.AMPERE)), //
		AUTOMATIC_GENERATOR_START(new Doc()//
				.options(AutomaticGeneratorStart.values())), //
		MANUAL_GENERATOR_CONTROL(new Doc()//
				.options(ManualGeneratorStart.values())), //
		GENERATOR_REQUEST_VIA_POWER_ON(new Doc()), //
		GENERATOR_SHUT_DOWN_LOAD_LIMIT(new Doc().unit(Unit.WATT)), //
		GENERATOR_START_UP_LOAD_LIMIT(new Doc().unit(Unit.WATT)), //
		FIRMWARE_VERSION_OF_THE_MAIN_PROCESSOR(new Doc()), //
		FIRMWARE_VERSION_OF_THE_LOGIC_COMPONENET(new Doc()), //
		GRID_CREATING_GENERATOR(new Doc()//
				.options(GridCreatingGenerator.values())), //
		RISE_IN_SELF_CONSUMPTION_SWITCHED_ON(new Doc()//
				.options(RiseInSelfConsumptionSwitchedOn.values())), //
		INITIATE_DEVICE_RESTART(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_BOOST_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FULL_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_EQUALIZATION_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FLOAT_CHARGE(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		FREQUENCY_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_MONITORING_GENERATOR_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		FREQUENCY_MONITORING_GENERATOR_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_GENERATOR_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER(new Doc()), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER_TRIPPING_TIME(new Doc().unit(Unit.SECONDS)), //
		NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		ACKNOWLEGDE_GENERATOR_ERRORS(new Doc()//
				.options(AcknowledgeGeneratorErrors.values())), //
		BATTERY_NOMINAL_CAPACITY(new Doc()), //
		OPERATING_MODE_OF_ACTIVE_POWER_LIMITATION_AT_OVERFREQUENCY(new Doc()//
				.options(OperatingModeOfActivePowerLimitationAtOverFrequency.values())), //
		DIFFERENCE_BETWEEN_STARTING_FREQ_AND_GRID_FREQ(new Doc().unit(Unit.HERTZ)), //
		DIFFERENCE_BETWEEN_RESET_FREQ_AND_GRID_FREQ(new Doc().unit(Unit.HERTZ)), //
		COSPHI_AT_STARTING_POINT(new Doc().unit(Unit.HERTZ)), //
		CONFIGURATION_OF_THE_COSPHI_STARTING_POINT(new Doc().unit(Unit.HERTZ)//
				.options(ConfigurationOfTheCosphiStartingPoint.values())), //
		COSPHI_AT_THE_END_POINT(new Doc().unit(Unit.HERTZ)), //
		CONFIGURATION_OF_THE_COSPHI_END_POINT(new Doc().unit(Unit.HERTZ)//
				.options(ConfigurationOfTheCosphiEndPoint.values())), //
		ACTIVE_POWER_AT_STARTING_POINT(new Doc().unit(Unit.PERCENT)), //
		ACTIVE_POWER_AT_END_POINT(new Doc().unit(Unit.PERCENT)), //
		BMS_OPERATING_MODE(new Doc()//
				.options(BMSOperatingMode.values())), //
		ACTIVE_POWER_GRADIENT_CONFIGURATION(new Doc().unit(Unit.PERCENT)), //
		GRID_REQUEST_VIA_POWER_SWITCH_ON(new Doc()//
				.options(GridRequestViPowerSwitchOn.values())), //
		GRID_REQUEST_SWITCH_ON_POWER_LIMIT(new Doc().unit(Unit.WATT)), //
		GRID_REQUEST_SWITCH_OFF_POWER_LIMIT(new Doc().unit(Unit.WATT)), //
		MANUAL_CONTROL_OF_NETWORK_CONNECTION(new Doc()//
				.options(ManualControlOfNetworkConnection.values())), //
		GRID_REQUEST_VIA_CHARGE_TYPE(new Doc()//
				.options(GridRequestViaChargeType.values())), //
		TYPE_OF_AC_SUBDISTRIBUTION(new Doc()//
				.options(TypeOfACSubdistribution.values())), //
		MANUAL_EQUAIZATION_CHARGE(new Doc()//
				.options(ManualEqualizationCharge.values())), //
		GENERATOR_REQUEST(new Doc()), //
		LIMIT_SOC_GENERATOR_START_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		LIMIT_SOC_GENERATOR_SHUTDOWN_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDTIONAL_TIME_RANGE_GENERATOR_REQUEST(new Doc()), //
		START_TIME_RANGE_FOR_GENERATOR_REQUEST(new Doc()), //
		LIMIT_SOC_GENERATOR_STOP_ADD_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		LIMIT_SOC_GENERATOR_START_ADD_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()//
				.options(TimeControlledGeneratorOperation.values())), //
		START_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()), //
		OPERATING_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()), //
		REPETITION_CYCLE_OF_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()//
				.options(RepetitionCycleOfTheTimeControlledGeneratorOperation.values())), //
		GENERATOR_REQUEST_WITH_SET_CHARGE_TYPE(new Doc()), //
		REACTION_TO_DIGITAL_INPUT_OF_GENERATOR_REQUEST(new Doc()), //
		AVERAGE_TIME_FOR_GENERATOR_REQUEST_VIA_POWER(new Doc().unit(Unit.SECONDS)), //
		AVERAGE_OPERATING_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		AVERAGE_IDLE_PERIOD_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		COOLING_DOWN_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		IDLE_PERIOD_AFTER_GENERATOR_FAULT(new Doc().unit(Unit.SECONDS)), //
		WARM_UP_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		GENERATOR_NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		TIME_CONTROLLED_INVERTER_OPERATION(new Doc()//
				.options(TimeControlledInverterOperation.values())), //
		START_TIME_FOR_TIME_CONTROLLED_INVERTER(new Doc()), //
		OPERATING_TIME_FOR_TIME_CONTROLLED_INVERTER(new Doc().unit(Unit.SECONDS)), //
		REPETITION_CYCLE_OF_TIME_CONTROLLED_INVERTER(new Doc()//
				.options(RepetitionCycleOfTheControlledInverter.values())), //
		DEVICE_NAME(new Doc()), //
		AUTOMATIC_UPDATES_ACTIVATED(new Doc()), //
		TIME_OF_THE_AUTOMATIC_UPDATE(new Doc()), //
		GRID_GUARD_VERSION(new Doc()), //
		MEMORY_CARD_STATUS(new Doc()//
				.options(MemoryCardStatus.values())), //
		UPDATE_VERSION_OF_THE_MAIN_PROCESSOR(new Doc()), //
		START_FEED_IN_PV(new Doc()), //
		STOP_FEED_IN_PV(new Doc()), //
		CUT_OFF_TIME_UNTIL_CONNECTION_TO_EXTERNAL_NETWORK(new Doc()), //
		AUTOMATIC_FREQUENCY_SYNCHRONIZATION(new Doc()//
				.options(AutomaticFrequencySynchronization.values())), //
		MAXIUMUM_CURRENT_FROM_PUBLIC_GRID(new Doc()), //
		POWER_FEEDBACK_TO_PUBLIC_GRID_ALLOWED(new Doc()//
				.options(PowerFeedbackToPublicGridAllowed.values())), //
		GRID_REQUEST_VIA_SOC_SWITCHED_ON(new Doc()), //
		LIMIT_SOC_FOR_CONNECTION_TO_GRID(new Doc()), //
		LIMIT_SOC_FOR_DISCONNECTION_FROM_GRID(new Doc()), //
		START_TIME_ADDTIONAL_TIME_RANGE_GRID_REQUEST(new Doc()), //
		START_INTERVAL_GRID_REQUEST(new Doc()), //
		LIMIT_SOC_FOR_CONNECT_TO_GRID_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_DISCONNECT_FROM_GRID_IN_ADD_TIME_RANGE(new Doc()), //
		ENERGY_SAVING_MODE_SWITCH_ON(new Doc()), //
		MAXIMUM_GRID_REVERSE_POWER(new Doc()), //
		MAXIMUM_GRID_REVERSE_POWER_TRIPPING_TIME(new Doc()), //
		TIME_UNTIL_CHANGE_OVER_TO_ENERGY_SAVING_MODE(new Doc()), //
		MAXIMUM_DURATION_OF_ENERGY_SAVING_MODE(new Doc()), //
		START_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL(new Doc()), //
		END_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL(new Doc()), //
		BATTERY_SOC_FOR_PROTECTION_MODE(new Doc()), //
		BATTERY_SWITCH_ONLIMIT_AFTER_OVER_TEMP_SHUT_DOWN(new Doc()), //
		OUTPUT_RESISTANCE_OF_BATTERY_CONNECTION(new Doc()), //
		LOWER_LIMIT_DEEP_DISCHARGE_PROTECT_AREA_PRIOR_SHUTDOWN(new Doc()), //
		MINIMUM_WIDTH_OF_DEEP_DISCHARGE_PROTECTION_AREA(new Doc()), //
		MINIMUM_WIDTH_OF_BAKCUP_POWER_AREA(new Doc()), //
		AREA_WIDTH_FOR_CONSERVING_SOC(new Doc()), //
		MINIMUM_WIDTH_OF_OWN_CONSUMPTION_AREA(new Doc()), //
		MOST_PRODUCTIVE_MONTH_FOR_BATTERY_USAGE_RANGE(new Doc()), //
		SEASON_OPERATION_ACTIVE(new Doc()), //
		VOLTAGE_SET_POINT_WITH_DEACTIVATED_BATTERY_MENAGEMENT(new Doc()), //
		CYCLE_TIME_FOR_FULL_CHARGE(new Doc()), //
		CYCLE_TIME_FOR_EQUALIZATION_CHARGE(new Doc()), //
		BATTERY_TEMPERATUR_COMPENSATION(new Doc()), //
		AUTOMATIC_EQUALIZATION_CHARGE(new Doc()), //
		TYPE_OF_ADDTIONAL_DC_SOURCES(new Doc()), //
		LIMITATION_TYPE_OF_GENERATOR_CURRENT(new Doc()), //
		SENSIVITY_OF_GENERATOR_FAILURE_DETECTION(new Doc()), //
		INVERTER_NOMINAL_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		INVERTER_NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		MAXIMUM_AC_BATTERY_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_1(new Doc().unit(Unit.PERCENT)), //
		LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_1(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_1(new Doc()), //
		TIME_LOAD_SHEDDING_1(new Doc()), //
		LIMIT_SOC_FOR_START_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_2(new Doc().unit(Unit.PERCENT)), //
		LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_2(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_2(new Doc()), //
		TIME_LOAD_SHEDDING_2(new Doc()), //
		LIMIT_SOC_FOR_START_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE(new Doc()), //
		CLUSTER_BEHAVIOUR_WHEN_A_DEVICE_FAILS(new Doc()), //
		COMMUNICATION_VERSION(new Doc()), //
		TIME_OUT_FOR_COMMUNICATION_FAULT_INDICATION(new Doc()), //
		ENERGY_SAVING_MODE(new Doc()), //
		UPDATE_VERSION_OF_THE_LOGIC_COMPONENT(new Doc()), //
		FIRMWARE_VERSION_OF_PROTOCOL_CONVERTER(new Doc()), //
		HARDWARE_VERSION_OF_PROTOCOL_CONVERTER(new Doc()), //
		SERIAL_NUMBER_OF_THE_PROTOCOL_CONVERTER(new Doc()), //

		BATTERY_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		DEBUG_SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT) //
//				 on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(SiChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(value);
					});
				})), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(new Doc()), //
		GRID_GUARD_CODE(new Doc()), //
		MIN_SOC_POWER_OFF(new Doc()), //
		SET_CONTROL_MODE(new Doc()//
				.options(SetControlMode.values())), //
		METER_SETTING(new Doc()//
				.options(MeterSetting.values())), //
		OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION(new Doc()//
				.options(OperatingModeForActivePowerLimitation.values())), //
		OPERATING_MODE_FOR_REACTIVE_POWER(new Doc()), //
		MAXIMUM_BATTERY_CHARGING_POWER_CAPACITY(new Doc()), //
		MAXIMUM_BATTERY_DISCHARGING_POWER_CAPACITY(new Doc()), //
		;

		private final Doc doc;

		private SiChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}