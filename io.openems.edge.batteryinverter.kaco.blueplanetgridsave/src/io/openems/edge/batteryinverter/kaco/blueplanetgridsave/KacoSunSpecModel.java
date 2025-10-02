package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.Point;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumBooleanPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumFieldPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumFieldPoint.SunSpecEnumPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;

public enum KacoSunSpecModel implements SunSpecModel {
	S_64201("Bidirectional inverter control", //
			"Bidirectional inverter control backend", //
			52 /* length */, KacoSunSpecModel.S64201.values(), SunSpecModelType.VENDOR_SPECIFIC), //
	S_64202("Battery Charge Discharge Characteristic", //
			"Bidirectional inverter battery charge discharge characteristic", //
			14 /* length */, KacoSunSpecModel.S64202.values(), SunSpecModelType.VENDOR_SPECIFIC), //
	S_64203("Batterysystem Information", //
			"Batterysystem Information Frontend", //
			26 /* length */, KacoSunSpecModel.S64203.values(), SunSpecModelType.VENDOR_SPECIFIC), //
	S_64204("Q(U) extended", //
			"Q(U) offset extension", //
			8 /* length */, KacoSunSpecModel.S64204.values(), SunSpecModelType.VENDOR_SPECIFIC //
	);

	public static enum S64201 implements SunSpecPoint {
		VERSION_MAJOR(new ValuePoint("S64201_VERSION_MAJOR", "Major Version of model", "", //
				ValuePoint.Type.UINT16, true /* mandatory? */, AccessMode.READ_ONLY, Unit.NONE)), //
		VERSION_MINOR(new ValuePoint("S64201_VERSION_MINOR", "Minor Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)),
		REQUESTED_STATE(new EnumPoint("S64201_REQUESTED_STATE", "Enumerated value. Control operating state", "", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64201RequestedState.values())), //
		CURRENT_STATE(new EnumPoint("S64201_CURRENT_STATE", "Enumerated value. Operating State", "", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_ONLY, S64201CurrentState.values())),
		CONTROL_MODE(new EnumPoint("S64201_CONTROL_MODE", "Power Control mode", "", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64201ControlMode.values())),
		RESERVED_5(new ReservedPoint("S64201_RESERVED_5")), //
		WATCHDOG(new ValuePoint("S64201_WATCHDOG", "Enable Watchdog", """
				Register must be written with the desired watchdog timeout in seconds. \
				Watchdog timer is reset on every write access to the value written \
				to the register. 0 means watchdog is disabled. It is recommended to \
				re-write the register at least once before half of the watchdog timeout \
				has elapsed.""", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.SECONDS)),
		W_SET_PCT(new ScaledValuePoint("S64201_W_SET_PCT", "Active power output setpoint (in percent of WMax)", //
				"negative values mean charge", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "W_SET_PCT_SF")), //
		VAR_SET_PCT(new ScaledValuePoint("S64201_VAR_SET_PCT", "Reactive power output setpoint (in percent of VAMax)", //
				"negative values mean charge", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "VAR_SET_PCT_SF")), //
		RESERVED_9(new ReservedPoint("S64201_RESERVED_9")), //
		RESERVED_10(new ReservedPoint("S64201_RESERVED_10")), //
		RESERVED_11(new ReservedPoint("S64201_RESERVED_11")), //
		RESERVED_12(new ReservedPoint("S64201_RESERVED_12")), //
		RESERVED_13(new ReservedPoint("S64201_RESERVED_13")), //
		ST_VND(new EnumFieldPoint("S64201_ST_VND", "PrologState", "", //
				EnumFieldPoint.Type.ENUMFIELD16, false, AccessMode.READ_ONLY, S64201StVnd.values())),
		ST_PU(new EnumPoint("S64201_ST_PU", "Power Unit State (DSP)", "", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_ONLY, S64201StPu.values())),
		ST_PCU(new EnumPoint("S64201_ST_PCU", "Precharge unit state", "", //
				EnumPoint.Type.ENUM16, false, AccessMode.READ_ONLY, S64201StPcu.values())),
		ERR_PCU(new EnumPoint("S64201_ERR_PCU", "Precharge unit error", "", //
				EnumPoint.Type.ENUM16, false, AccessMode.READ_ONLY, S64201ErrPcu.values())),
		WPARAM_RMP_TMS(new ValuePoint("S64201_WPARAM_RMP_TMS", "WparamRmpDecTmm", //
				"The time of the PT1 in seconds (time to accomplish a change of 99,3% which means 5tau)"
						+ " for active power (W) in response to changes of WSetPct.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.MILLISECONDS)), //
		WPARAM_RMP_DEC_TMM(new ScaledValuePoint("S64201_WPARAM_RMP_DEC_TMM", "WparamRmpDecTmm", //
				"The maximum rate at which the active power (W) value may be decreased in response to changes of WSetPct", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "RMP_INC_DEC_SF")), //
		WPARAM_RMP_INC_TMM(new ScaledValuePoint("S64201_WPARAM_RMP_INC_TMM", "WparamRmpIncTmm", //
				"The maximum rate at which the active power (W) value may be increased in response to changes of WSetPct", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "RMP_INC_DEC_SF")), //
		RESERVED_21(new ReservedPoint("S64201_RESERVED_21")), //
		RESERVED_22(new ReservedPoint("S64201_RESERVED_22")), //
		W_PARAM_ENA(new EnumPoint("S64201_W_PARAM_ENA", "WSet_Ena control", //
				"Enumerated value. Enable filter and ramp rate parameters for active power setpoint (W)", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64201WParamEna.values())), //
		VAR_PARAM_RMP_TMS(new ScaledValuePoint("S64201_VAR_PARAM_RMP_TMS", "VarParamRmpTms", //
				"The time of the PT1 in seconds (time to accomplish a change of 99,3% which means 5tau) "
						+ "for reactive power (var) in response to changes of VarSetPct.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.SECONDS, "RMP_TMS_SF")), //
		VAR_PARAM_RMP_DEC_TMM(new ScaledValuePoint("S64201_VAR_PARAM_RMP_DEC_TMM", "VarParamRmpDecTmm", //
				"The maximum rate at which the reactive power (var) may be decreased in response to changes of VarSetPct.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "RMP_INC_DEC_SF")), //
		VAR_PARAM_RMP_INC_TMM(new ScaledValuePoint("S64201_VAR_PARAM_RMP_INC_TMM", "VarParamRmpIncTmm", //
				"The maximum rate at which the reactive power (var) may be increased in response to changes of VarSetPct.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "RMP_INC_DEC_SF")), //
		RESERVED_27(new ReservedPoint("S64201_RESERVED_27")), //
		RESERVED_28(new ReservedPoint("S64201_RESERVED_28")), //
		VAR_PARAM_ENA(new EnumPoint("S64201_VAR_PARAM_ENA", "VarParam_Ena", //
				"Enumerated value. Enable filter and ramp rate parameters for reactive power setpoint (var)", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64201VarParamEna.values())), //
		PH_VPH_A(new ScaledValuePoint("S64201_PH_VPH_A", "Phase Voltage AN", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_B(new ScaledValuePoint("S64201_PH_VPH_B", "Phase Voltage BN", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_C(new ScaledValuePoint("S64201_PH_VPH_C", "Phase Voltage CN", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.VOLT, "V_SF")), //
		W(new ScaledValuePoint("S64201_W", "AC Power", "", //
				ValuePoint.Type.INT16, true, AccessMode.READ_ONLY, Unit.WATT, "W_SF")), //
		V_AR(new ScaledValuePoint("S64201_V_AR", "AC Reactive Power", "", //
				ValuePoint.Type.INT16, true, AccessMode.READ_ONLY, Unit.VOLT_AMPERE_REACTIVE, "V_AR_SF")), //
		HZ(new ScaledValuePoint("S64201_HZ", "Line Frequency", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.HERTZ, "Hz_SF")), //
		RESERVED_36(new ReservedPoint("S64201_RESERVED_36")), //
		RESERVED_37(new ReservedPoint("S64201_RESERVED_37")), //
		RESERVED_38(new ReservedPoint("S64201_RESERVED_38")), //
		RESERVED_39(new ReservedPoint("S64201_RESERVED_39")), //
		RESERVED_40(new ReservedPoint("S64201_RESERVED_40")), //
		RESERVED_41(new ReservedPoint("S64201_RESERVED_41")), //
		RESERVED_42(new ReservedPoint("S64201_RESERVED_42")), //
		RESERVED_43(new ReservedPoint("S64201_RESERVED_43")), //
		W_SET_PCT_SF(
				new ScaleFactorPoint("S64201_W_SET_PCT_SF", "Scale factor for active power setpoint (% WMax)", "")), //
		VAR_SET_PCT_SF(new ScaleFactorPoint("S64201_VAR_SET_PCT_SF",
				"Scale factor for reactive power setpoint (% VAMax)", "")), //
		RMP_TMS_SF(new ScaleFactorPoint("S64201_RMP_TMS_SF", "Scale factor for PT1", "")), //
		RMP_INC_DEC_SF(
				new ScaleFactorPoint("S64201_RMP_INC_DEC_SF", "Scale factor for increment and decrement ramps.", "")), //
		V_SF(new ScaleFactorPoint("S64201_V_SF", "Scale factor for voltage measurements", "")), //
		W_SF(new ScaleFactorPoint("S64201_W_SF", "Scale factor for active power measurement", "")), //
		V_AR_SF(new ScaleFactorPoint("S64201_V_AR_SF", "Scale factor for reactive power measurement", "")), //
		HZ_SF(new ScaleFactorPoint("S64201_HZ_SF", "Scale factor for frequency measurement", ""));

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

		public static enum S64201StVnd implements SunSpecEnumPoint {
			UNDEFINED(-1, "Undefined"), //
			WAITING_FOR_FEED_IN(1, "waitingForFeedIn", Level.INFO), //
			INSUFFICIENT_BATTERY_VOLTAGE(2, "insufficientBatteryVoltage"),
			YIELD_COUNTER_FOR_DAILY(4, "yieldCounterForDaily"), //
			SELF_TEST_IN_PROGR_CHECK(8, "selfTestInProgrCheck", Level.INFO), //
			TEMPERATURE_IN_UNIT_TOO(10, "temperatureInUnitToo", Level.WARNING), //
			POWER_LIMITATION_IF_THE(11, "powerLimitationIfThe", Level.INFO), //
			POWADORPROTECT_DISCONNECTION(17, "powadorprotectDisconnection", Level.WARNING), //
			RESID_CURRENT_SHUTDOWN(18, "residCurrentShutdown", Level.FAULT), //
			GENERATOR_INSULATION(19, "generatorInsulation", Level.FAULT), //
			ACTIVE_RAMP_LIMITATION(20, "activeRampLimitation", Level.INFO), //
			VOLTAGE_TRANS_FAULT_CURRENT(30, "voltageTransFaultCurrent", Level.INFO), //
			SELF_TEST_ERROR_THE_INTERNAL(32, "selfTestErrorTheInternal", Level.FAULT), //
			DC_FEEDIN_ERROR_THE_DC(33, "dcFeedinErrorTheDc"),
			INTERNAL_COMMUNICATION(34, "internalCommunication", Level.FAULT), //
			PROTECTION_SHUTDOWN_SW(35, "protectionShutdownSw", Level.FAULT), //
			PROTECTION_SHUTDOWN_HW(36, "protectionShutdownHw", Level.FAULT), //
			ERROR_GENERATOR_VOLTAGE(38, "errorGeneratorVoltage"), //
			LINE_FAILURE_UNDERVOLTAGE_1(41, "lineFailureUndervoltage1", Level.FAULT), //
			LINE_FAILURE_OVERVOLTAGE_1(42, "lineFailureOvervoltage1", Level.FAULT), //
			LINE_FAILURE_UNDERVOLTAGE_2(43, "lineFailureUndervoltage2", Level.FAULT), //
			LINE_FAILURE_OVERVOLTAGE_2(44, "lineFailureOvervoltage2", Level.FAULT), //
			LINE_FAILURE_UNDERVOLTAGE_3(45, "lineFailureUndervoltage3", Level.FAULT), //
			LINE_FAILURE_OVERVOLTAGE_3(46, "lineFailureOvervoltage3", Level.FAULT), //
			GRID_FAILURE_PHASETOPHASE(47, "gridFailurePhaseToPhase", Level.FAULT), //
			LINE_FAILURE_UNDERFREQ(48, "lineFailureUnderfreq", Level.FAULT), //
			LINE_FAILURE_OVERFREQ(49, "lineFailureOverfreq", Level.FAULT), //
			LINE_FAILURE_AVERAGE(50, "lineFailureAverage", Level.WARNING), //
			WAITING_FOR_REACTIVATION(57, "waitingForReactivation"), //
			CONTROL_BOARD_OVERTEMP(58, "controlBoardOvertemp", Level.FAULT), //
			SELF_TEST_ERROR_A_FAULT(59, "selfTestErrorAFault", Level.FAULT), //
			GENERATOR_VOLTAGE_TOO(60, "generatorVoltageToo"), //
			EXTERNAL_LIMIT_X_THE(61, "externalLimitXThe"), //
			P_F_FREQUENCYDEPENDENT(63, "pFFrequencydependent"), //
			OUTPUT_CURRENT_LIMITING(64, "outputCurrentLimiting"), //
			FAULT_AT_POWER_SECTION(67, "faultAtPowerSection", Level.FAULT), //
			FAN_1_ERROR_THE_FAN_IS(70, "fan1ErrorTheFanIs", Level.FAULT), //
			STANDALONE_GRID_ERR_STANDALONE(73, "standaloneGridErrStandalone"), //
			EXTERNAL_IDLE_POWER_REQUIREMENT(74, "externalIdlePowerRequirement"), //
			SELFTEST(75, "selftest"), //
			INSULATION_MEASUREMENT(79, "insulationMeasurement"), //
			INSULATION_MEAS_NOT_POSSIBLE(80, "insulationMeasNotPossible"), //
			PROTECTION_SHUTDOWN_LINE_1(81, "protectionShutdownLine1", Level.FAULT), //
			PROTECTION_SHUTDOWN_LINE_2(82, "protectionShutdownLine2", Level.FAULT), //
			PROTECTION_SHUTDOWN_LINE_3(83, "protectionShutdownLine3", Level.FAULT), //
			PROTECTION_SHUTDOWN_UNDERVOLT(84, "protectionShutdownUndervolt", Level.FAULT), //
			PROTECT_SHUTDOWN_OVERVOLT(85, "protectShutdownOvervolt", Level.FAULT), //
			PROTECT_SHUTDOWN_DC_LINK(86, "protectShutdownDcLink", Level.FAULT), //
			PROTECT_SHUTDOWN_OVERCURRENT_1(87, "protectShutdownOvercurrent1", Level.FAULT), //
			PROTECT_SHUTDOWN_OVERCURRENT_2(88, "protectShutdownOvercurrent2", Level.FAULT), //
			PROTECT_SHUTDOWN_OVERCURRENT_3(89, "protectShutdownOvercurrent3", Level.FAULT), //
			BUFFER_1_SELF_TEST_ERROR(93, "buffer1SelfTestError", Level.FAULT), //
			SELF_TEST_ERROR_BUFFER(94, "selfTestErrorBuffer", Level.FAULT), //
			RELAY_1_SELF_TEST_ERROR(95, "relay1SelfTestError", Level.FAULT), //
			RELAY_2_SELF_TEST_ERROR(96, "relay2SelfTestError", Level.FAULT), //
			PROTECTION_SHUTDOWN_OVERCURRENT(97, "protectionShutdownOvercurrent", Level.FAULT), //
			PROTECT_SHUTDOWN_HW_GTE(98, "protectShutdownHwGte", Level.FAULT), //
			PROTECT_SHUTDOWN_HW_BUFFER(99, "protectShutdownHwBuffer", Level.FAULT), //
			PROTECT_SHUTDOWN_HW_OVERHEATING(100, "protectShutdownHwOverheating", Level.FAULT), //
			PLAUSIBILITY_FAULT_AFI(104, "plausibilityFaultAfi", Level.FAULT), //
			PLAUSIBILITY_FAULT_RELAY(105, "plausibilityFaultRelay", Level.FAULT), //
			PLAUSIBILITY_ERROR_DCDC(106, "plausibilityErrorDcdc"), //
			CHECK_SURGE_PROTECTION(107, "checkSurgeProtection"), //
			EXTERNAL_COMMUNICATION(196, "externalCommunication", Level.WARNING), //
			SYMMETRY_ERROR_PARALLEL(197, "symmetryErrorParallel"), //
			BATTERY_DISCONNECTED(198, "batteryDisconnected"), //
			BATTERY_CONSTRAINTS_MISSING(199, "batteryConstraintsMissing"), //
			WAITING_FOR_FAULT_ACKNOWLEDGEMENT(215, "waitingForFaultAcknowledgement", Level.WARNING), //
			PRECHARGE_UNIT_FAULT(218, "prechargeUnitFault", Level.FAULT), //
			READY_FOR_PRECHARGING(219, "readyForPrecharging"), //
			PRECHARGE_PRECHARGE_UNIT(220, "prechargePrechargeUnit"), //
			WAIT_FOR_COOLDOWN_TIME(221, "waitForCooldownTime"), //
			DISCHARGE_LIMITS_REACHED(222, "dischargeLimitsReached"), //
			CHARGING_LIMITS_REACHED(223, "chargeLimitsReached"), //
			BATTERY_VOLTAGE_TOO_LOW(225, "batteryVoltageTooLow", Level.WARNING), //
			PROTECTIVE_SHUTDOWN_CURRENT_ASYMMETRY(227, "protectiveShutdownCurrentAsymmetry", Level.WARNING), //
			PROTECTIVE_SHUTDOWN_VOLTAGE_ASYMMETRY(228, "protectiveShutdownVoltageAsymmetry", Level.WARNING), //
			DC_VOLTAGE_ERROR(229, "dcVoltageError", Level.WARNING)//
			;

			private final EnumBooleanPoint point;

			private S64201StVnd(int value, String channelKey, Level level) {
				this.point = new EnumBooleanPoint(//
						value, //
						this.getClass().getSimpleName().toUpperCase() + "_" + this.name(), //
						level, //
						KacoSunSpecModel.class, //
						channelKey);
			}

			private S64201StVnd(int value, String text) {
				this.point = new EnumBooleanPoint(//
						value, //
						this.getClass().getSimpleName().toUpperCase() + "_" + this.name());
			}

			@Override
			public int getValue() {
				return this.point.value;
			}

			@Override
			public String getName() {
				return this.point.name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}

			@Override
			public EnumBooleanPoint get() {
				return this.point;
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

		protected final Point impl;

		private S64201(Point impl) {
			this.impl = impl;
		}

		@Override
		public Point get() {
			return this.impl;
		}
	}

	// TODO Registers DIS_MIN_V to EN_LIMIT are repeated blocks
	public static enum S64202 implements SunSpecPoint {
		VERSION_MAJOR(new ValuePoint("S64202_VERSION_MAJOR", "Major Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		VERSION_MINOR(new ValuePoint("S64202_VERSION_MINOR", "Minor Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		RESERVED_2(new ReservedPoint("S64202_RESERVED_2")), //
		RESERVED_3(new ReservedPoint("S64202_RESERVED_3")), //
		V_SF(new ScaleFactorPoint("S64202_V_SF", "", "")), //
		A_SF(new ScaleFactorPoint("S64202_A_SF", "", "")), //
		DIS_MIN_V_0(new ScaledValuePoint("S64202_DIS_MIN_V_0", "min. discharge voltage", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.VOLT, "V_SF")), //
		DIS_MAX_A_0(new ScaledValuePoint("S64202_DIS_MAX_A_0", "max. discharge current", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.AMPERE, "A_SF")), //
		DIS_CUTOFF_A_0(new ScaledValuePoint("S64202_DIS_CUTOFF_A_0", "discharge cutoff current", //
				"Disconnect if discharge current lower than DisCutoffA. No auto disconnect if value is 0", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.AMPERE, "A_SF")), //
		CHA_MAX_V_0(new ScaledValuePoint("S64202_CHA_MAX_V_0", "max. charge voltage", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.VOLT, "V_SF")), //
		CHA_MAX_A_0(new ScaledValuePoint("S64202_CHA_MAX_A_0", "max. charge current", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.AMPERE, "A_SF")), //
		CHA_CUTOFF_A_0(new ScaledValuePoint("S64202_CHA_CUTOFF_A_0", "charge cutoff current", //
				"Disconnect if charge current lower than ChaCutoffA. No auto disconnect if value is 0", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.AMPERE, "A_SF")), //
		RESERVED_0(new ReservedPoint("S64202_RESERVED_0")), //
		EN_LIMIT_0(new EnumPoint("S64202_EN_LIMIT_0", "EnLimit", //
				"new battery limits are activated when EnLimit is 1. Must be 0 or 1", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64202EnLimit.values()));

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

		protected final Point impl;

		private S64202(Point impl) {
			this.impl = impl;
		}

		@Override
		public Point get() {
			return this.impl;
		}
	}

	// TODO Registers BAT_ID to BAT_SW_SUM are repeated blocks
	public static enum S64203 implements SunSpecPoint {
		VERSION_MAJOR(new ValuePoint("S64203_VERSION_MAJOR", "Major Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		VERSION_MINOR(new ValuePoint("S64203_VERSION_MINOR", "Minor Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		EMS_ERR_CODE(new EnumPoint("S64203_EMS_ERR_CODE", "Errorcode from EMS", "", //
				EnumPoint.Type.ENUM16, false, AccessMode.READ_WRITE, S64203EmsErrCode.values())),
		SOC_SF(new ScaleFactorPoint("S64203_SOC_SF", "SoC scale factor", "")), //
		SOH_SF(new ScaleFactorPoint("S64203_SOH_SF", "SoH scale factor", "")), //
		TEMP_SF(new ScaleFactorPoint("S64203_TEMP_SF", "temperature scale factor", "")), //
		BAT_ID_0(new ValuePoint("S64203_BAT_ID_0", "Battery ID or Serial number", "", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.VOLT)), //
		BAT_SOC_0(new ScaledValuePoint("S64203_BAT_SOC_0", "SoC of battery", //
				"valid range is from 0 to 100", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.PERCENT, "SOC_SF")), //
		BAT_SOH_0(new ScaledValuePoint("S64203_BAT_SOH_0", "SoH of battery", //
				"valid range is from 0 to 100", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.PERCENT, "SOH_SF")), //
		BAT_TEMP_0(new ScaledValuePoint("S64203_BAT_TEMP_0", "Avg. temperature of battery",
				"valid range is from -50 to 100", //
				ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.DEGREE_CELSIUS, "TEMP_SF")), //
		BAT_SW_VER_0(new ValuePoint("S64203_BAT_SW_VER_0", "Softwareversion of battery", "", //
				ValuePoint.Type.STRING4, false, AccessMode.READ_WRITE, Unit.NONE)), //
		BAT_SW_SUM_0(new ValuePoint("S64203_BAT_SW_SUM_0", "Software checksum of battery", "", //
				ValuePoint.Type.STRING4, false, AccessMode.READ_WRITE, Unit.NONE)), //
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

		protected final Point impl;

		private S64203(Point impl) {
			this.impl = impl;
		}

		@Override
		public Point get() {
			return this.impl;
		}
	}

	public static enum S64204 implements SunSpecPoint {
		VERSION_MAJOR(new ValuePoint("S64204_VERSION_MAJOR", "Major Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		VERSION_MINOR(new ValuePoint("S64204_VERSION_MINOR", "Minor Version of model", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		V_OFF_PCT(new ScaledValuePoint("S64204_V_OFF_PCT", "Q(U) grid voltage offset", "", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "V_OFF_PCT_SF")), //
		VAR_OFF_PCT(new ScaledValuePoint("S64204_VAR_OFF_PCT", "VArOffPct", //
				"Q(U) reactive power offset (The depending reference is configured in model 126. %refVal is %Wmax, %VArMax or %VArAval depending on value of DeptRef in model 126)", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "VAR_OFF_PCT_SF")), //
		RVRT_TMS(new ValuePoint("S64204_RVRT_TMS", "Timeout period for volt-VAR curve selection", "", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.SECONDS)), //
		OFFSET_ENA(new EnumPoint("S64204_OFFSET_ENA", "Q(U) dynamic Q/U offset enable", "", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64204OffsetEna.values())), //
		V_OFF_PCT_SF(new ScaleFactorPoint("S64204_V_OFF_PCT_SF", "Scale factor for offset voltage", "")), //
		VAR_OFF_PCT_SF(new ScaleFactorPoint("S64204_VAR_OFF_PCT_SF", "Scale factor for reactive power offset", ""));

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

		protected final Point impl;

		private S64204(Point impl) {
			this.impl = impl;
		}

		@Override
		public Point get() {
			return this.impl;
		}
	}

	public final String label;
	public final String description;
	public final int length;
	public final SunSpecPoint[] points;
	public final SunSpecModelType modelType;

	private KacoSunSpecModel(String label, String description, int length, SunSpecPoint[] points,
			SunSpecModelType modelType) {
		this.label = label;
		this.description = description;
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

	private static class ReservedPoint extends ValuePoint {
		protected ReservedPoint(String channelId) {
			super(channelId, "Reserved", "", ValuePoint.Type.PAD, false, AccessMode.READ_ONLY, Unit.NONE);
		}
	}

}
