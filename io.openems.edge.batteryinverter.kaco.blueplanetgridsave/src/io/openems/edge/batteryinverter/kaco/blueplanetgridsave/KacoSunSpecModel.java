package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointImpl;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointType;

public enum KacoSunSpecModel implements SunSpecModel {
	S_64201(//
			"Bidirectional inverter control", //
			"Bidirectional inverter control backend", //
			"", //
			52, //
			KacoSunSpecModel.S64201.values(), //
			SunSpecModelType.VENDOR_SPECIFIC), //
	S_64202(//
			"Battery Charge Discharge Characteristic", //
			"Bidirectional inverter battery charge discharge characteristic", //
			"", //
			14, //
			KacoSunSpecModel.S64202.values(), //
			SunSpecModelType.VENDOR_SPECIFIC), //
	S_64203(//
			"Batterysystem Information", //
			"Batterysystem Information Frontend", //
			"", //
			26, //
			KacoSunSpecModel.S64203.values(), //
			SunSpecModelType.VENDOR_SPECIFIC), //
	S_64204(//
			"Q(U) extended", //
			"Q(U) offset extension", //
			"", //
			8, //
			KacoSunSpecModel.S64204.values(), //
			SunSpecModelType.VENDOR_SPECIFIC //
	); //

	public static enum S64201 implements SunSpecPoint {
		VERSION_MAJOR(new PointImpl(//
				"S64201_VERSION_MAJOR", //
				"Version", //
				"Major Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VERSION_MINOR(new PointImpl(//
				"S64201_VERSION_MINOR", //
				"VerMinor", //
				"Minor Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		REQUESTED_STATE(new PointImpl(//
				"S64201_REQUESTED_STATE", //
				"RequestedState", //
				"Enumerated value. Control operating state", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64201RequestedState.values())), //
		CURRENT_STATE(new PointImpl(//
				"S64201_CURRENT_STATE", //
				"CurrentState", //
				"Enumerated value. Operating State", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201CurrentState.values())),
		CONTROL_MODE(new PointImpl(//
				"S64201_CONTROL_MODE", //
				"ControlMode", //
				"Power Control mode", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64201ControlMode.values())),
		RESERVED_5(new ReservedPointImpl("S64201_RESERVED_5")), //
		WATCHDOG(new PointImpl(//
				"S64201_WATCHDOG", //
				"Watchdog", //
				"Enable Watchdog", //
				"""
						Register must be written with the desired watchdog timeout in seconds. \
						Watchdog timer is reset on every write access to the value written \
						to the register. 0 means watchdog is disabled. It is recommended to \
						re-write the register at least once before half of the watchdog timeout \
						has elapsed.""", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		W_SET_PCT(new PointImpl(//
				"S64201_W_SET_PCT", //
				"WSetPct", //
				"Active power output setpoint (in percent of WMax)", //
				"negative values mean charge", //
				PointType.INT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SET_PCT_SF", //
				new OptionsEnum[0])), //
		VAR_SET_PCT(new PointImpl(//
				"S64201_VAR_SET_PCT", //
				"VarSetPct", //
				"Reactive power output setpoint (in percent of VAMax)", //
				"negative values mean charge", //
				PointType.INT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"VAR_SET_PCT_SF", //
				new OptionsEnum[0])), //
		RESERVED_9(new ReservedPointImpl("S64201_RESERVED_9")), //
		RESERVED_10(new ReservedPointImpl("S64201_RESERVED_10")), //
		RESERVED_11(new ReservedPointImpl("S64201_RESERVED_11")), //
		RESERVED_12(new ReservedPointImpl("S64201_RESERVED_12")), //
		RESERVED_13(new ReservedPointImpl("S64201_RESERVED_13")), //
		ST_VND(new PointImpl(//
				"S64201_ST_VND", //
				"StVnd", //
				"PrologState", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201StVnd.values())),
		ST_PU(new PointImpl(//
				"S64201_ST_PU", //
				"StPu", //
				"Power Unit State (DSP)", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201StPu.values())),
		ST_PCU(new PointImpl(//
				"S64201_ST_PCU", //
				"StPcu", //
				"Precharge unit state", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201StPcu.values())),
		ERR_PCU(new PointImpl(//
				"S64201_ERR_PCU", //
				"ErrPcu", //
				"Precharge unit error", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201ErrPcu.values())),
		WPARAM_RMP_TMS(new PointImpl(//
				"S64201_WPARAM_RMP_TMS", //
				"WparamRmpTms", //
				"The time of the PT1 in seconds (time to accomplish a change of 99,3% which means 5tau)"
						+ " for active power (W) in response to changes of WSetPct.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				"RMP_TMS_SF", //
				new OptionsEnum[0])), //
		WPARAM_RMP_DEC_TMM(new PointImpl(//
				"S64201_WPARAM_RMP_DEC_TMM", //
				"WparamRmpDecTmm", //
				"The maximum rate at which the active power (W) value may be decreased in response to changes of WSetPct", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"RMP_INC_DEC_SF", //
				new OptionsEnum[0])), //
		WPARAM_RMP_INC_TMM(new PointImpl(//
				"S64201_WPARAM_RMP_INC_TMM", //
				"WparamRmpIncTmm", //
				"The maximum rate at which the active power (W) value may be increased in response to changes of WSetPct", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"RMP_INC_DEC_SF", //
				new OptionsEnum[0])), //
		RESERVED_21(new ReservedPointImpl("S64201_RESERVED_21")), //
		RESERVED_22(new ReservedPointImpl("S64201_RESERVED_22")), //
		W_PARAM_ENA(new PointImpl(//
				"S64201_W_PARAM_ENA", //
				"WParamEna", //
				"Enumerated value. Enable filter and ramp rate parameters for active power setpoint (W)", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64201WParamEna.values())), //
		VAR_PARAM_RMP_TMS(new PointImpl(//
				"S64201_VAR_PARAM_RMP_TMS", //
				"VarParamRmpTms", //
				"The time of the PT1 in seconds (time to accomplish a change of 99,3% which means 5tau) "
						+ "for reactive power (var) in response to changes of VarSetPct.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				"RMP_TMS_SF", //
				new OptionsEnum[0])), //
		VAR_PARAM_RMP_DEC_TMM(new PointImpl(//
				"S64201_VAR_PARAM_RMP_DEC_TMM", //
				"VarParamRmpDecTmm", //
				"The maximum rate at which the reactive power (var) may be decreased in response to changes of VarSetPct.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"RMP_INC_DEC_SF", //
				new OptionsEnum[0])), //
		VAR_PARAM_RMP_INC_TMM(new PointImpl(//
				"S64201_VAR_PARAM_RMP_INC_TMM", //
				"VarParamRmpDecTmm", //
				"The maximum rate at which the reactive power (var) may be increased in response to changes of VarSetPct.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"RMP_INC_DEC_SF", //
				new OptionsEnum[0])), //
		RESERVED_27(new ReservedPointImpl("S64201_RESERVED_27")), //
		RESERVED_28(new ReservedPointImpl("S64201_RESERVED_28")), //
		VAR_PARAM_ENA(new PointImpl(//
				"S64201_VAR_PARAM_ENA", //
				"VarParamEna", //
				"Enumerated value. Enable filter and ramp rate parameters for reactive power setpoint (var)", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64201VarParamEna.values())), //
		PH_VPH_A(new PointImpl(//
				"S64201_PH_VPH_A", //
				"Phase Voltage AN", //
				"Phase Voltage AN", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"V_SF", //
				new OptionsEnum[0])), //
		PH_VPH_B(new PointImpl(//
				"S64201_PH_VPH_B", //
				"Phase Voltage BN", //
				"Phase Voltage BN", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"V_SF", //
				new OptionsEnum[0])), //
		PH_VPH_C(new PointImpl(//
				"S64201_PH_VPH_C", //
				"Phase Voltage CN", //
				"Phase Voltage CN", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"V_SF", //
				new OptionsEnum[0])), //
		W(new PointImpl(//
				"S64201_W", //
				"Watts", //
				"AC Power", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"W_SF", //
				new OptionsEnum[0])), //
		V_AR(new PointImpl(//
				"S64201_V_AR", //
				"VAr", //
				"AC Reactive Power", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.VOLT_AMPERE_REACTIVE, //
				"V_AR_SF", //
				new OptionsEnum[0])), //
		HZ(new PointImpl(//
				"S64201_HZ", //
				"Hz", //
				"Line Frequency", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		RESERVED_36(new ReservedPointImpl("S64201_RESERVED_36")), //
		RESERVED_37(new ReservedPointImpl("S64201_RESERVED_37")), //
		RESERVED_38(new ReservedPointImpl("S64201_RESERVED_38")), //
		RESERVED_39(new ReservedPointImpl("S64201_RESERVED_39")), //
		RESERVED_40(new ReservedPointImpl("S64201_RESERVED_40")), //
		RESERVED_41(new ReservedPointImpl("S64201_RESERVED_41")), //
		RESERVED_42(new ReservedPointImpl("S64201_RESERVED_42")), //
		RESERVED_43(new ReservedPointImpl("S64201_RESERVED_43")), //
		W_SET_PCT_SF(new PointImpl(//
				"S64201_W_SET_PCT_SF", //
				"WSetPct_SF", //
				"Scale factor for active power setpoint (% WMax)", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VAR_SET_PCT_SF(new PointImpl(//
				"S64201_VAR_SET_PCT_SF", //
				"VarSetPct_SF", //
				"Scale factor for reactive power setpoint (% VAMax)", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		RMP_TMS_SF(new PointImpl(//
				"S64201_RMP_TMS_SF", //
				"RmpTms_SF", //
				"Scale factor for PT1", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		RMP_INC_DEC_SF(new PointImpl(//
				"S64201_RMP_INC_DEC_SF", //
				"RmpIncDec_SF", //
				"Scale factor for increment and decrement ramps.", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		V_SF(new PointImpl(//
				"S64201_V_SF", //
				"V_SF", //
				"Scale factor for voltage measurements", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		W_SF(new PointImpl(//
				"S64201_W_SF", //
				"W_SF", //
				"Scale factor for active power measurement", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		V_AR_SF(new PointImpl(//
				"S64201_V_AR_SF", //
				"VAr_SF", //
				"Scale factor for reactive power measurement", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		HZ_SF(new PointImpl(//
				"S64201_HZ_SF", //
				"Hz_SF", //
				"Scale factor for frequency measurement", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		; //

		public static enum S64201RequestedState implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			OFF(1, "Shutdown system and discharge the DC-Link / Clear non pending error"), //
			STANDBY(8, "Charge DC-Link / Disconnect from grid"), //
			GRID_PRE_CONNECTED(10, "Prepare for grid connection"), //
			GRID_CONNECTED(11, "Connect to grid"); //

			private final int value;
			private final String name;

			private S64201RequestedState(int value, String name) {
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

		public static enum S64201CurrentState implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			OFF(1, "Not operating"), //
			SLEEPING(2, "Sleeping/auto-shudown"), //
			STARTING(3, "Starting up"), //
			MPPT(4, "MPP tracking"), //
			THROTTLED(5, "Reduced power output"), //
			SHUTTING_DOWN(6, "Shutting down"), //
			FAULT(7, "Fault"), //
			STANDBY(8, "Standby"), //
			PRECHARGE(9, "DC precharge"), //
			GRID_PRE_CONNECTED(10, "Preparing grid connection"), //
			GRID_CONNECTED(11, "Grid connected"), //
			NO_ERROR_PENDING(12, "Wait for error acknowledge"); //

			private final int value;
			private final String name;

			private S64201CurrentState(int value, String name) {
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

		public static enum S64201ControlMode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			SUNSPEC_CTRL_MODE_NONE(0, "Use reactive power control modes as configured in the device settings"), //
			SUNSPEC_CTRL_MODE_QFIX(1, "Use reactive power setpoint VarSetPct"); //

			private final int value;
			private final String name;

			private S64201ControlMode(int value, String name) {
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

		public static enum S64201StVnd implements OptionsEnum {
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
			RESID_CURRENT_SHUTDOWN(18,
					"Resid. current shutdown Residual current was detected. The feed-in was interrupted."), //
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
			LINE_FAILURE_UNDERFREQ(48,
					"Line failure: underfreq. Grid frequency is too low. This fault may be gridrelated."), //
			LINE_FAILURE_OVERFREQ(49,
					"Line failure: overfreq. Grid frequency is too high. This fault may be gridrelated."), //
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

			private S64201StVnd(int value, String name) {
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

		public static enum S64201StPu implements OptionsEnum {
			UNDEFINED(-1, "Undefined"); //
			// for internal use only

			private final int value;
			private final String name;

			private S64201StPu(int value, String name) {
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

		public static enum S64201StPcu implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			BOOTING(1, "Boot up and waiting configuration"), //
			STANDBY(2, "Idle state, ready for precharge"), //
			PRECHARGE_1(3, "Precharge Step 1; close negative power relay before starting precharge"), //
			PRECHARGE_2(4, "Precharge Step 2; close precharge relay and start precharge"), //
			PRECHARGE_3(5, "Precharge Step 3; close positive power relay after successful precharge"), //
			RUNNING(6, "Running; precharge finished, ready for operation"), //
			COOLDOWN(7,
					"Cooldown of resistor; precharge resistor needs to cooldown bevor new precharge sequence can be started"), //
			ERROR(8, "Error; see ErrPcu for details"), //
			NO_ERROR_PENDING(9, "no error is pending; error state can be cleared by setting RequestedState=1"); //

			private final int value;
			private final String name;

			private S64201StPcu(int value, String name) {
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

		public static enum S64201ErrPcu implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			NO_ERROR(0, "No Error"), //
			OVER_TEMP(1, "Over temperature"), //
			OVER_VOLT(2, "Input voltage (DC) too high"), //
			UNDER_VOLT(3, "Input voltage (DC) too low"), //
			BATT_POL_INCORRECT(4, "Input voltage reverse polarity protection"), //
			ERROR_COUNTER_TOO_HIGH(5,
					"Max. number of unsuccessful (ERROR_PRECHARGE) precharge tries exceeded; "
							+ "Contact authorized service. Error can only be cleared by AC power cycling"), //
			ERROR_PRECHARGE(6, "Precharge not successful"), //
			ERROR_RUNNING_MODE(7, "Error during RUNNING mode"), //
			I2C_COMM(8, "I2C communication error"), //
			CAN_COMM(9, "CAN communication error"), //
			EXT_SWITCHOFF(10, "external switch off, eg. because of gridfault"), //
			BATTERY_LIMITS_NA(11, "Battery limits never set by EMS"); //

			private final int value;
			private final String name;

			private S64201ErrPcu(int value, String name) {
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

		public static enum S64201WParamEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64201WParamEna(int value, String name) {
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

		public static enum S64201VarParamEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64201VarParamEna(int value, String name) {
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

		protected final PointImpl impl;

		private S64201(PointImpl impl) {
			this.impl = impl;
		}

		@Override
		public PointImpl get() {
			return this.impl;
		}
	}

	// TODO Registers DIS_MIN_V to EN_LIMIT are repeated blocks
	public static enum S64202 implements SunSpecPoint {
		VERSION_MAJOR(new PointImpl(//
				"S64202_VERSION_MAJOR", //
				"Version", //
				"Major Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VERSION_MINOR(new PointImpl(//
				"S64202_VERSION_MINOR", //
				"VerMinor", //
				"Minor Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		RESERVED_2(new ReservedPointImpl("S64202_RESERVED_2")), //
		RESERVED_3(new ReservedPointImpl("S64202_RESERVED_3")), //
		V_SF(new PointImpl(//
				"S64202_V_SF", //
				"V_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		A_SF(new PointImpl(//
				"S64202_A_SF", //
				"A_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		DIS_MIN_V_0(new PointImpl(//
				"S64202_DIS_MIN_V_0", //
				"min. discharge voltage", //
				"min. discharge voltage", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.VOLT, //
				"V_SF", //
				new OptionsEnum[0])), //
		DIS_MAX_A_0(new PointImpl(//
				"S64202_DIS_MAX_A_0", //
				"max. discharge current", //
				"max. discharge current", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.AMPERE, //
				"A_SF", //
				new OptionsEnum[0])), //
		DIS_CUTOFF_A_0(new PointImpl(//
				"S64202_DIS_CUTOFF_A_0", //
				"discharge cutoff current", //
				"Disconnect if discharge current lower than DisCutoffA", //
				"no auto disconnect if value is 0", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.AMPERE, //
				"A_SF", //
				new OptionsEnum[0])), //
		CHA_MAX_V_0(new PointImpl(//
				"S64202_CHA_MAX_V_0", //
				"max. charge voltage", //
				"max. charge voltage", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.VOLT, //
				"V_SF", //
				new OptionsEnum[0])), //
		CHA_MAX_A_0(new PointImpl(//
				"S64202_CHA_MAX_A_0", //
				"max. charge current", //
				"max. charge current", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.AMPERE, //
				"A_SF", //
				new OptionsEnum[0])), //
		CHA_CUTOFF_A_0(new PointImpl(//
				"S64202_CHA_CUTOFF_A_0", //
				"charge cutoff current", //
				"Disconnect if charge current lower than ChaCutoffA", //
				"no auto disconnect if value is 0", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.AMPERE, //
				"A_SF", //
				new OptionsEnum[0])), //
		RESERVED_0(new ReservedPointImpl("S64202_RESERVED_0")), //
		EN_LIMIT_0(new PointImpl(//
				"S64202_EN_LIMIT_0", //
				"EnLimit", //
				"new battery limits are activated when EnLimit is 1", //
				"must be 0 or 1", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64202EnLimit.values())), //
		; //

		public static enum S64202EnLimit implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			NO_ACTIVATE(0, "No Activate"), //
			ACTIVATE(1, "Activate"); //

			private final int value;
			private final String name;

			private S64202EnLimit(int value, String name) {
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

		protected final PointImpl impl;

		private S64202(PointImpl impl) {
			this.impl = impl;
		}

		@Override
		public PointImpl get() {
			return this.impl;
		}
	}

	// TODO Registers BAT_ID to BAT_SW_SUM are repeated blocks
	public static enum S64203 implements SunSpecPoint {
		VERSION_MAJOR(new PointImpl(//
				"S64203_VERSION_MAJOR", //
				"Version", //
				"Major Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VERSION_MINOR(new PointImpl(//
				"S64203_VERSION_MINOR", //
				"VerMinor", //
				"Minor Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		E_M_S_ERR_CODE(new PointImpl(//
				"S64203_E_M_S_ERR_CODE", //
				"Errorcode from EMS", //
				"Minor Version of model", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64203EmsErrCode.values())),
		SOC_SF(new PointImpl(//
				"S64203_SOC_SF", //
				"SoC_SF", //
				"SoC scale factor", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		SOH_SF(new PointImpl(//
				"S64203_SOH_SF", //
				"SoH_SF", //
				"SoH scale factor", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		TEMP_SF(new PointImpl(//
				"S64203_TEMP_SF", //
				"Temp_SF", //
				"temperature scale factor", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		BAT_ID_0(new PointImpl(//
				"S64203_BAT_ID_0", //
				"Battery ID / Serial number", //
				"Battery ID or Serial number", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.VOLT, //
				null, //
				new OptionsEnum[0])), //
		BAT_SOC_0(new PointImpl(//
				"S64203_BAT_SOC_0", //
				"SoC of battery", //
				"SoC of battery", //
				"valid range is from 0 to 100", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"SOC_SF", //
				new OptionsEnum[0])), //
		BAT_SOH_0(new PointImpl(//
				"S64203_BAT_SOH_0", //
				"SoH of battery", //
				"SoH of battery", //
				"valid range is from 0 to 100", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"SOH_SF", //
				new OptionsEnum[0])), //
		BAT_TEMP_0(new PointImpl(//
				"S64203_BAT_TEMP_0", //
				"Avg. temperature of battery", //
				"Avg. temperature of battery", //
				"valid range is from -50 to 100", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.DEGREE_CELSIUS, //
				"TEMP_SF", //
				new OptionsEnum[0])), //
		BAT_SW_VER_0(new PointImpl(//
				"S64203_BAT_SW_VER_0", //
				"Softwareversion of battery", //
				"Softwareversion of battery", //
				"", //
				PointType.STRING4, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		BAT_SW_SUM_0(new PointImpl(//
				"S64203_BAT_SW_SUM_0", //
				"Software checksum of battery", //
				"Software checksum of battery", //
				"", //
				PointType.STRING4, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		; //

		public static enum S64203EmsErrCode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"); //

			private final int value;
			private final String name;

			private S64203EmsErrCode(int value, String name) {
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

		protected final PointImpl impl;

		private S64203(PointImpl impl) {
			this.impl = impl;
		}

		@Override
		public PointImpl get() {
			return this.impl;
		}
	}

	public static enum S64204 implements SunSpecPoint {
		VERSION_MAJOR(new PointImpl(//
				"S64204_VERSION_MAJOR", //
				"Version", //
				"Major Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VERSION_MINOR(new PointImpl(//
				"S64204_VERSION_MINOR", //
				"VerMinor", //
				"Minor Version of model", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		V_OFF_PCT(new PointImpl(//
				"S64204_V_OFF_PCT", //
				"VOffPct", //
				"Q(U) grid voltage offset", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"V_OFF_PCT_SF", //
				new OptionsEnum[0])), //
		VAR_OFF_PCT(new PointImpl(//
				"S64204_VAR_OFF_PCT", //
				"VArOffPct", //
				"Q(U) reactive power offset (The depending reference is configured in model 126. "
						+ "%refVal is %Wmax, %VArMax or %VArAval depending on value of DeptRef in model 126)", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"VAR_OFF_PCT_SF", //
				new OptionsEnum[0])), //
		RVRT_TMS(new PointImpl(//
				"S64204_RVRT_TMS", //
				"RvrtTms", //
				"Timeout period for volt-VAR curve selection", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		OFFSET_ENA(new PointImpl(//
				"S64204_OFFSET_ENA", //
				"OffsetEna", //
				"Q(U) dynamic Q/U offset enable", //
				"", //
				PointType.ENUM16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				S64204OffsetEna.values())), //
		V_OFF_PCT_SF(new PointImpl(//
				"S64204_V_OFF_PCT_SF", //
				"VOffPct_SF", //
				"Scale factor for offset voltage", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		VAR_OFF_PCT_SF(new PointImpl(//
				"S64204_VAR_OFF_PCT_SF", //
				"VarOffPct_SF", //
				"Scale factor for reactive power offset", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])) //
		; //

		public static enum S64204OffsetEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64204OffsetEna(int value, String name) {
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

		protected final PointImpl impl;

		private S64204(PointImpl impl) {
			this.impl = impl;
		}

		@Override
		public PointImpl get() {
			return this.impl;
		}
	}

	public final String label;
	public final String description;
	public final String notes;
	public final int length;
	public final SunSpecPoint[] points;
	public final SunSpecModelType modelType;

	private KacoSunSpecModel(String label, String description, String notes, int length, SunSpecPoint[] points,
			SunSpecModelType modelType) {
		this.label = label;
		this.description = description;
		this.notes = notes;
		this.length = length;
		this.points = points;
		this.modelType = modelType;
	}

	@Override
	public SunSpecPoint[] points() {
		return this.points;
	}

	@Override
	public String label() {
		return this.label;
	}

	private static class ReservedPointImpl extends PointImpl {
		protected ReservedPointImpl(String channelId) {
			super(channelId, //
					"Reserved", //
					"", //
					"", //
					PointType.PAD, //
					false, //
					AccessMode.READ_ONLY, //
					Unit.NONE, //
					null, //
					new OptionsEnum[0]);
		}
	}

}
