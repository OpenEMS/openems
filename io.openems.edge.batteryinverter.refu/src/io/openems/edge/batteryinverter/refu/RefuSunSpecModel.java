package io.openems.edge.batteryinverter.refu;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.Point;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;

/**
 * SunSpec models specific to REFU inverters.
 *
 * <ul>
 * <li>S_123: Immediate Controls — same 24-register layout as the standard model
 * but with signed INT16 for W_MAX_LIM_PCT and VAR_W_MAX_PCT, because REFU uses
 * negative values to express charge power.
 * <li>S_64800: MESA-PCS vendor extension .
 * </ul>
 */
public enum RefuSunSpecModel implements SunSpecModel {
	S_123("Immediate Controls", //
			"Immediate Inverter Controls (REFU: signed W_MAX_LIM_PCT / VAR_W_MAX_PCT)", //
			24/* length */, RefuSunSpecModel.S123.values(), SunSpecModelType.INVERTER), //
	S_64800("MESA-PCS Extensions", //
			"REFU MESA-PCS vendor extension: state control, heartbeat, battery current limits", //
			14/* length */, RefuSunSpecModel.S64800.values(), SunSpecModelType.VENDOR_SPECIFIC); //

	// Model 123 — Immediate Controls (REFU variant)
	// 24 data registers — identical register layout to DefaultSunSpecModel.S_123
	// but W_MAX_LIM_PCT and reactive power setpoints are INT16 (signed).
	public static enum S123 implements SunSpecPoint {
		CONN_WIN_TMS(new ValuePoint("S123_CONN_WIN_TMS", "Conn_WinTms", //
				"Time window for connect/disconnect.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		CONN_RVRT_TMS(new ValuePoint("S123_CONN_RVRT_TMS", "Conn_RvrtTms", //
				"Timeout period for connect/disconnect.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		CONN(new EnumPoint("S123_CONN", "Conn", //
				"Enumerated value. Connection control.", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S123Conn.values())), //
		W_MAX_LIM_PCT(new ScaledValuePoint("S123_W_MAX_LIM_PCT", "WMaxLimPct", //
				"Power output setpoint as % WMax. Negative = charge.", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.PERCENT, "W_MAX_LIM_PCT_SF")), //
		W_MAX_LIM_PCT_WIN_TMS(new ValuePoint("S123_W_MAX_LIM_PCT_WIN_TMS", "WMaxLimPct_WinTms", //
				"Time window for power limit change.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		W_MAX_LIM_PCT_RVRT_TMS(new ValuePoint("S123_W_MAX_LIM_PCT_RVRT_TMS", "WMaxLimPct_RvrtTms", //
				"Timeout period for power limit.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		W_MAX_LIM_PCT_RMP_TMS(new ValuePoint("S123_W_MAX_LIM_PCT_RMP_TMS", "WMaxLimPct_RmpTms", //
				"Ramp time for moving from current setpoint to new setpoint.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		W_MAX_LIM_ENA(new EnumPoint("S123_W_MAX_LIM_ENA", "WMaxLim_Ena", //
				"Throttle enable/disable control.", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S123Ena.values())), //
		OUT_PF_SET(new ScaledValuePoint("S123_OUT_PF_SET", "OutPFSet", //
				"Set power factor to specified value.", //
				ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.NONE, "OUT_PF_SET_SF")), //
		OUT_PF_SET_WIN_TMS(new ValuePoint("S123_OUT_PF_SET_WIN_TMS", "OutPFSet_WinTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		OUT_PF_SET_RVRT_TMS(new ValuePoint("S123_OUT_PF_SET_RVRT_TMS", "OutPFSet_RvrtTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		OUT_PF_SET_RMP_TMS(new ValuePoint("S123_OUT_PF_SET_RMP_TMS", "OutPFSet_RmpTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		OUT_PF_SET_ENA(new EnumPoint("S123_OUT_PF_SET_ENA", "OutPFSet_Ena", //
				"", EnumPoint.Type.ENUM16, false, AccessMode.READ_WRITE, S123Ena.values())), //
		VAR_W_MAX_PCT(new ScaledValuePoint("S123_VAR_W_MAX_PCT", "VArWMaxPct", //
				"Reactive power as % WMax. Signed.", //
				ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.PERCENT, "VAR_PCT_SF")), //
		VAR_MAX_PCT(new ScaledValuePoint("S123_VAR_MAX_PCT", "VArMaxPct", //
				"", ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.PERCENT, "VAR_PCT_SF")), //
		VAR_AVAL_PCT(new ScaledValuePoint("S123_VAR_AVAL_PCT", "VArAvalPct", //
				"", ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.PERCENT, "VAR_PCT_SF")), //
		VAR_PCT_WIN_TMS(new ValuePoint("S123_VAR_PCT_WIN_TMS", "VArPct_WinTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		VAR_PCT_RVRT_TMS(new ValuePoint("S123_VAR_PCT_RVRT_TMS", "VArPct_RvrtTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		VAR_PCT_RMP_TMS(new ValuePoint("S123_VAR_PCT_RMP_TMS", "VArPct_RmpTms", //
				"", ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.SECONDS)), //
		VAR_PCT_MOD(new EnumPoint("S123_VAR_PCT_MOD", "VArPct_Mod", //
				"", EnumPoint.Type.ENUM16, false, AccessMode.READ_WRITE, S123VArPctMod.values())), //
		VAR_PCT_ENA(new EnumPoint("S123_VAR_PCT_ENA", "VArPct_Ena", //
				"Percent limit VAr enable/disable control.", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S123Ena.values())), //
		W_MAX_LIM_PCT_SF(new ScaleFactorPoint("S123_W_MAX_LIM_PCT_SF", "WMaxLimPct_SF", "")), //
		OUT_PF_SET_SF(new ScaleFactorPoint("S123_OUT_PF_SET_SF", "OutPFSet_SF", "")), //
		VAR_PCT_SF(new ScaleFactorPoint("S123_VAR_PCT_SF", "VArPct_SF", "")); //

		public static enum S123Conn implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISCONNECT(0, "Disconnect"), //
			CONNECT(1, "Connect"); //

			private final int value;
			private final String name;

			private S123Conn(int value, String name) {
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

		public static enum S123Ena implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLED(0, "Disabled"), //
			ENABLED(1, "Enabled"); //

			private final int value;
			private final String name;

			private S123Ena(int value, String name) {
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

		public static enum S123VArPctMod implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			W_MAX(1, "% WMax"), //
			VAR_MAX(2, "% VArMax"), //
			VAR_AVAL(3, "% VArAval"); //

			private final int value;
			private final String name;

			private S123VArPctMod(int value, String name) {
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

		private final Point point;

		private S123(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}

	// Model 64800 — MESA-PCS Vendor Extension, 14 registers
	public static enum S64800 implements SunSpecPoint {
		LOC_REM_CTL(new EnumPoint("S64800_LOC_REM_CTL", "LocRemCtl", //
				"0 = Remote (controller drives setpoints); 1 = Local.", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_ONLY, S64800LocRemCtl.values())), //
		PCS_HB(new ValuePoint("S64800_PCS_HB", "PcsHB", //
				"PCS heartbeat — incremented each cycle by the device.", //
				ValuePoint.Type.INT16, true, AccessMode.READ_ONLY, Unit.NONE)), //
		CONTROLLER_HB(new ValuePoint("S64800_CONTROLLER_HB", "ControllerHB", //
				"Controller heartbeat — must be incremented each cycle by the controller (watchdog).", //
				ValuePoint.Type.INT16, true, AccessMode.READ_WRITE, Unit.NONE)), //
		RESERVED_3(new ReservedPoint("S64800_RESERVED_3")), //
		PCS_SET_OPERATION(new EnumPoint("S64800_PCS_SET_OPERATION", "PcsSetOperation", //
				"Command to control PCS operating mode.", //
				EnumPoint.Type.ENUM16, true, AccessMode.READ_WRITE, S64800PcsSetOperation.values())), //
		MAX_BAT_A_CHA(new ScaledValuePoint("S64800_MAX_BAT_A_CHA", "MaxBatACha", //
				"Maximum battery charge current limit forwarded from the BMS.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.AMPERE, "MAX_BAT_A_SF")), //
		MAX_BAT_A_DISCHA(new ScaledValuePoint("S64800_MAX_BAT_A_DISCHA", "MaxBatADischa", //
				"Maximum battery discharge current limit forwarded from the BMS.", //
				ValuePoint.Type.UINT16, true, AccessMode.READ_WRITE, Unit.AMPERE, "MAX_BAT_A_SF")), //
		MAX_A(new ValuePoint("S64800_MAX_A", "MaxA", //
				"Maximum AC current.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.AMPERE)), //
		MAX_A_CUR(new ValuePoint("S64800_MAX_A_CUR", "MaxACur", //
				"Maximum AC current as percentage of rated.", //
				ValuePoint.Type.UINT16, false, AccessMode.READ_WRITE, Unit.PERCENT)), //
		MAX_BAT_A_SF(new ScaleFactorPoint("S64800_MAX_BAT_A_SF", "MaxBatA_SF", "")), //
		MAX_A_SF(new ScaleFactorPoint("S64800_MAX_A_SF", "MaxA_SF", "")), //
		MAX_A_CUR_SF(new ScaleFactorPoint("S64800_MAX_A_CUR_SF", "MaxACur_SF", "")), //
		PADDING_1(new ValuePoint("S64800_PADDING_1", "Padding1", "", //
				ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.NONE)), //
		PADDING_2(new ValuePoint("S64800_PADDING_2", "Padding2", "", //
				ValuePoint.Type.INT16, false, AccessMode.READ_WRITE, Unit.NONE)); //

		public static enum S64800LocRemCtl implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			REMOTE(0, "PCS Remote Control"), //
			LOCAL(1, "PCS Local Control"); //

			private final int value;
			private final String name;

			private S64800LocRemCtl(int value, String name) {
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

		public static enum S64800PcsSetOperation implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			NO_COMMAND(0, "No Command"), //
			START_PCS(1, "Connect to grid"), //
			STOP_PCS(2, "Stop system"), //
			ENTER_STANDBY_MODE(3, "Enter Standby Mode"), //
			EXIT_STANDBY_MODE(4, "Exit Standby Mode"); //

			private final int value;
			private final String name;

			private S64800PcsSetOperation(int value, String name) {
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

		private final Point point;

		private S64800(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}

	public final String label;
	public final String description;
	public final int length;
	public final SunSpecPoint[] points;
	public final SunSpecModelType modelType;

	private RefuSunSpecModel(String label, String description, int length, SunSpecPoint[] points,
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
