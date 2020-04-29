package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.ISunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointImpl;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointType;

public enum KacoSunSpecModel implements ISunSpecModel {
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
				S64201_RequestedState.values())), //
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
				S64201_CurrentState.values())),
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
				S64201_ControlMode.values())),
		RESERVED_5(new ReservedPointImpl("S64201_RESERVED_5")), //
		WATCHDOG(new PointImpl(//
				"S64201_WATCHDOG", //
				"Watchdog", //
				"Enable Watchdog", //
				"Register must be written with the desired watchdog timeout in seconds." //
						+ "Watchdog timer is reset on every write access to the value written " //
						+ "to the register. 0 means watchdog is disabled. It is recommended to" //
						+ "re-write the register at least once before half of the watchdog timeout" //
						+ "has elapsed.", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		W_SET_PCT(new PointImpl(//
				"S101_W_SET_PCT", //
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
				"S101_VAR_SET_PCT", //
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
				S64201_StVnd.values())),
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
				S64201_StPu.values())),
		ST_PCU(new PointImpl(//
				"S64201_ST_PCU", //
				"StPu", //
				"Precharge unit state", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S64201_StPcu.values())),
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
				S64201_ErrPcu.values())),
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
				S64201_WParamEna.values())), //
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
				"S64201_VAR_PARAM_RMP_DEC_TMM", //
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
				S64201_VarParamEna.values())), //
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

		public static enum S64201_RequestedState implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			OFF(1, "Shutdown system and discharge the DC-Link / Clear non pending error"), //
			STANDBY(8, "Charge DC-Link / Disconnect from grid"), //
			GRID_PRE_CONNECTED(10, "Prepare for grid connection"), //
			GRID_CONNECTED(11, "Connect to grid"); //

			private final int value;
			private final String name;

			private S64201_RequestedState(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_CurrentState implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			OFF(1, "Device is not operating"), //
			SLEEPING(2, "Device is sleeping / auto-shudown"), //
			STARTING(3, "Device is starting up"), //
			MPPT(4, "Device is auto tracking maximum power point"), //
			THROTTLED(5, "Device is operating at reduced power output"), //
			SHUTTING_DOWN(6, "Device is shutting down"), //
			FAULT(7, "One or more faults exist"), //
			STANDBY(8, "Device is in standby mode"), //
			PRECHARGE(9, "DC-Link is precharged"), //
			GRID_PRE_CONNECTED(10, "Device is prepared for grid connection"), //
			GRID_CONNECTED(11, "Device is connected to the grid"), //
			NO_ERROR_PENDING(12, "Device is waiting until the user clears the error which is not peding any more"); //

			private final int value;
			private final String name;

			private S64201_CurrentState(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_ControlMode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			SUNSPEC_CTRL_MODE_NONE(0, "Use reactive power control modes as configured in the device settings"), //
			SUNSPEC_CTRL_MODE_QFIX(1, "Use reactive power setpoint VarSetPct"); //

			private final int value;
			private final String name;

			private S64201_ControlMode(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_StVnd implements OptionsEnum {
			UNDEFINED(-1, "Undefined"); //
			// TODO "see device manual for description of all possible states"

			private final int value;
			private final String name;

			private S64201_StVnd(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_StPu implements OptionsEnum {
			UNDEFINED(-1, "Undefined"); //
			// for internal use only

			private final int value;
			private final String name;

			private S64201_StPu(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_StPcu implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			BOOTING(1, "Boot up and waiting configuration"), //
			STANDBY(2, "Idle state, ready for precharge"), //
			PRECHARGE_1(3, "Precharge Step 1; close negative power relay before starting precharge"), //
			PRECHARGE_2(4, "Precharge Step 2; close precharge relay and start precharge"), //
			PRECHARGE_3(5, "Precharge Step 3; close positiv power relay after successful precharge"), //
			RUNNING(6, "Running; precharge finished, ready for operation"), //
			COOLDOWN(7,
					"Cooldown of resistor; precharge resistor needs to cooldown bevor new precharge sequence can be started"), //
			ERROR(8, "Error; see ErrPcu for details"), //
			NO_ERROR_PENDING(9, "no error is pending; error state can be cleared by setting RequestedState=1"); //

			private final int value;
			private final String name;

			private S64201_StPcu(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_ErrPcu implements OptionsEnum {
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
			EXT_SWITCHOFF(10, "external swich off, eg. because of gridfault"), //
			BATTERY_LIMITS_NA(11, "Battery limits never set by EMS"); //

			private final int value;
			private final String name;

			private S64201_ErrPcu(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_WParamEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64201_WParamEna(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public static enum S64201_VarParamEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64201_VarParamEna(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
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
				S64202_EnLimit.values())), //
		; //

		public static enum S64202_EnLimit implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			NO_ACTIVATE(0, "No Activate"), //
			ACTIVATE(1, "Activate"); //

			private final int value;
			private final String name;

			private S64202_EnLimit(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
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
				S64203_EmsErrCode.values())),
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
				"V_SF", //
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

		public static enum S64203_EmsErrCode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"); //

			private final int value;
			private final String name;

			private S64203_EmsErrCode(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
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
				S64204_OffsetEna.values())), //
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

		public static enum S64204_OffsetEna implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(2, "Enabled"); //

			private final int value;
			private final String name;

			private S64204_OffsetEna(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return value;
			}

			@Override
			public String getName() {
				return name;
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
