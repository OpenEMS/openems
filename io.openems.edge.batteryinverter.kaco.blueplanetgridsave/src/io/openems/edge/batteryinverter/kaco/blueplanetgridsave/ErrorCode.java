package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.types.OptionsEnum;

public enum ErrorCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAITING_FOR_FEED_IN(1, "Self-test: Grid parameters and generator voltage are being checked"), //
	BATTERY_VOLTAGE_TOO_LOW(2, "Battery Voltage too low! Transition from or to 'Standby'"), //
	YIELD_COUNTER_FOR_DAILY(4, "Yield counter for daily and annual yields are displayed"), //
	SELF_TEST_IN_PROGR_CHECK(8,
			"Self test in progr. Check the shutdown of the power electronics as well as the shutdown of the grid relay before the charge process."), //
	TEMPERATURE_IN_UNIT_TOO(10,
			"Temperature in unit too high In the event of overheating, the device shuts down. Possible causes: ambient temperature too high, fan covered, device fault."), //
	POWER_LIMITATION_IF_THE(11,
			"Power limitation: If the generator power is too high, the device limits itself to the maximum power (e.g. around noon if the generator capacity is too large). "), //
	POWADORPROTECT_DISCONNECTION(17,
			"Powador-protect disconnection The activated grid and system protection has been tripped."), //
	RESID_CURRENT_SHUTDOWN(18, "Resid. current shutdown Residual current was detected. The feed-in was interrupted."), //
	GENERATOR_INSULATION(19,
			"Generator insulation fault Insulation fault Insulation resistance from DC-/DC + to PE too low"), //
	ACTIVE_RAMP_LIMITATION(20,
			"Active ramp limitation The result when the power is increased with a ramp is country-specific."), //
	VOLTAGE_TRANS_FAULT_CURRENT(30,
			"Voltage trans. fault Current and voltage measurement in the device are not plausible."), //
	SELF_TEST_ERROR_THE_INTERNAL(32,
			"Self test error The internal grid separation relay test has failed. Notify your authorised electrician if the fault occurs repeatedly!"), //
	DC_FEEDIN_ERROR_THE_DC(33,
			"DC feed-in error The DC feed-in has exceeded the permitted value. This DC feed-in can be caused in the device by grid conditions and may not necessarily indicate a fault."), //
	INTERNAL_COMMUNICATION(34,
			"Internal communication error A communication error has occurred in the internal data transmission. "), //
	PROTECTION_SHUTDOWN_SW(35,
			"Protection shutdown SW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "), //
	PROTECTION_SHUTDOWN_HW(36,
			"Protection shutdown HW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "), //
	ERROR_GENERATOR_VOLTAGE(38, "Error: Generator Voltage too high Error: Battery overvoltage"), //
	LINE_FAILURE_UNDERVOLTAGE_1(41,
			"Line failure undervoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	LINE_FAILURE_OVERVOLTAGE_1(42,
			"Line failure overvoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	LINE_FAILURE_UNDERVOLTAGE_2(43,
			"Line failure undervoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	LINE_FAILURE_OVERVOLTAGE_2(44,
			"Line failure overvoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	LINE_FAILURE_UNDERVOLTAGE_3(45,
			"Line failure undervoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	LINE_FAILURE_OVERVOLTAGE_3(46,
			"Line failure overvoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."), //
	GRID_FAILURE_PHASETOPHASE(47, "Grid failure phase-to-phase voltage"), //
	LINE_FAILURE_UNDERFREQ(48, "Line failure: underfreq. Grid frequency is too low. This fault may be gridrelated."), //
	LINE_FAILURE_OVERFREQ(49, "Line failure: overfreq. Grid frequency is too high. This fault may be gridrelated."), //
	LINE_FAILURE_AVERAGE(50,
			"Line failure: average voltage The grid voltage measurement according to EN 50160 has exceeded the maximum permitted limit value. This fault may be grid-related."), //
	WAITING_FOR_REACTIVATION(57,
			"Waiting for reactivation Waiting time of the device following an error. The devices switches on after a countryspecific waiting period."), //
	CONTROL_BOARD_OVERTEMP(58,
			"Control board overtemp. The temperature inside the unit was too high. The device shuts down to avoid hardware damage. "), //
	SELF_TEST_ERROR_A_FAULT(59,
			"Self test error A fault occurred during a self-test. Contact a qualified electrician."), //
	GENERATOR_VOLTAGE_TOO(60, "Generator voltage too high Battery voltage too high"), //
	EXTERNAL_LIMIT_X_THE(61,
			"External limit x% The grid operator has activated the external PowerControl limit. The inverter limits the power."), //
	P_F_FREQUENCYDEPENDENT(63,
			"P(f)/frequency-dependent power reduction: When certain country settings are activated, the frequency-dependent power reduction is activated."), //
	OUTPUT_CURRENT_LIMITING(64,
			"Output current limiting: The AC current is limited once the specified maximum value has been reached."), //
	FAULT_AT_POWER_SECTION(67,
			"Fault at power section 1 There is a fault in the power section. Contact a qualified electrician."), //
	FAN_1_ERROR_THE_FAN_IS(70,
			"Fan 1 error The fan is malfunctioning. Replace defective fan See Maintenance and troubleshooting chapter."), //
	STANDALONE_GRID_ERR_STANDALONE(73, "Standalone grid err. Standalone mode was detected."), //
	EXTERNAL_IDLE_POWER_REQUIREMENT(74,
			"External idle power requirement The grid operator limits the feed-in power of the device via the transmitted reactive power factor."), //
	SELFTEST(75, "Selftest in progress"), //
	INSULATION_MEASUREMENT(79, "Insulation measurement PV generator's insulation is being measured"), //
	INSULATION_MEAS_NOT_POSSIBLE(80,
			"Insulation meas. not possible The insulation measurement cannot be performed because the generator voltage is too volatile. - "), //
	PROTECTION_SHUTDOWN_LINE_1(81,
			"Protection shutdown line volt. L1 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."), //
	PROTECTION_SHUTDOWN_LINE_2(82,
			"Protection shutdown line volt. L2 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."), //
	PROTECTION_SHUTDOWN_LINE_3(83,
			"Protection shutdown line volt. L3 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."), //
	PROTECTION_SHUTDOWN_UNDERVOLT(84,
			"Protection shutdown undervolt. DC link A voltage deviation has been found in the DC link. An internal protective mechanism has disconnected the device to protect it against damage. In a TN-C-S grid, the PE must be connected to the device and at the same time the PEN bridge in the device must be removed. In case of repeated occurrence: Contact a qualified electrician."), //
	PROTECT_SHUTDOWN_OVERVOLT(85, "Protect. shutdown overvolt. DC link"), //
	PROTECT_SHUTDOWN_DC_LINK(86, "Protect. shutdown DC link asymmetry"), //
	PROTECT_SHUTDOWN_OVERCURRENT_1(87, "Protect. shutdown overcurrent L1"), //
	PROTECT_SHUTDOWN_OVERCURRENT_2(88, "Protect. shutdown overcurrent L2"), //
	PROTECT_SHUTDOWN_OVERCURRENT_3(89, "Protect. shutdown overcurrent L3"), //
	BUFFER_1_SELF_TEST_ERROR(93,
			"Buffer 1 self test error The control board is defective. Please inform your electrician/system manufacturer's service department."), //
	SELF_TEST_ERROR_BUFFER(94,
			"Self test error buffer 2 The control board is defective. Notify authorised electrician / KACO Service!"), //
	RELAY_1_SELF_TEST_ERROR(95, "Relay 1 self test error The power section is defective. Notify KACO Service"), //
	RELAY_2_SELF_TEST_ERROR(96,
			"Relay 2 self test error The power section is defective. Please inform your electrician/system manufacturer's service department."), //
	PROTECTION_SHUTDOWN_OVERCURRENT(97,
			"Protection shutdown overcurrent HW Too much power has been fed into the grid. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."), //
	PROTECT_SHUTDOWN_HW_GATE(98,
			"Protect. shutdown HW gate driver An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."), //
	PROTECT_SHUTDOWN_HW_BUFFER(99,
			"Protect. shutdown HW buffer free An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."), //
	PROTECT_SHUTDOWN_HW_OVERHEATING(100,
			"Protect. shutdown HW overheating The device has been switched off because the temperatures in the housing were too high. Check to make sure that the fans are working. Replace fan if necessary."), //
	PLAUSIBILITY_FAULT_AFI(104,
			"Plausibility fault AFI module The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"), //
	PLAUSIBILITY_FAULT_RELAY(105,
			"Plausibility fault relay The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"), //
	PLAUSIBILITY_ERROR_DCDC(106, "Plausibility error DCDC converter"), //
	CHECK_SURGE_PROTECTION(107,
			"Check surge protection device Surge protection device (if present in the device) has tripped and must be reset if appropriate."), //
	EXTERNAL_COMMUNICATION(196, "External communication error"), //
	SYMMETRY_ERROR_PARALLEL(197,
			"Symmetry error parallel connection Circuit currents too high for two or more parallel connected bidirectional feed-in inverters. Synchronise intermediate circuit of the parallel connected devices and synchronise the symmetry."), //
	BATTERY_DISCONNECTED(198,
			"Battery disconnected Connection to the battery disconnected. Check connection. The battery voltage may be outside the parameterised battery limits."), //
	BATTERY_CONSTRAINTS_MISSING(199, "Battery constraints are missing // Batteriegrenzen nicht vorhanden"), //
	WAITING_FOR_FAULT_ACKNOWLEDGEMENT(215, "Waiting for fault acknowledgement by EMS"), //
	PRECHARGE_UNIT_FAULT(218, "Precharge unit fault Precharge unit: Group fault for precharge unit"), //
	READY_FOR_PRECHARGING(219, "Ready for precharging Precharge unit: Ready for precharging"), //
	PRECHARGE_PRECHARGE_UNIT(220, "Precharge Precharge unit: Precharge process being carried out"), //
	WAIT_FOR_COOLDOWN_TIME(221,
			"Wait for cooldown time Precharge unit: Precharge resistance requires time to cool down"), //
	CURRENTLY_UNKNOWN(222, "State is currently unknown"), //
	CHARGE_RANGES_REACHEDX(223, "Charge ranges are reached");

	private final int value;
	private final String name;

	private ErrorCode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}