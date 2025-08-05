package io.openems.edge.solaredge.ess.common;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint.Type.ENUM16;
import static io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint.Type.ACC32;
import static io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint.Type.INT16;
import static io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint.Type.STRING16;
import static io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint.Type.STRING8;
import static io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint.Type.UINT16;

import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.Point;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint;

public enum SolarEdgeSunSpecModel implements SunSpecModel {
	S_1("Common", //
			"All SunSpec compliant devices must include this as the first model", //
			66 /* length */, SolarEdgeSunSpecModel.S1.values(), SunSpecModelType.COMMON), //
	S_101("Inverter (Single Phase)", //
			"Include this model for single phase inverter monitoring", //
			50 /* length */, SolarEdgeSunSpecModel.S101.values(), SunSpecModelType.INVERTER), //
	S_102("Inverter (Split-Phase)", //
			"Include this model for split phase inverter monitoring", //
			50 /* length */, SolarEdgeSunSpecModel.S102.values(), SunSpecModelType.INVERTER), //
	S_103("Inverter (Three Phase)", //
			"Include this model for three phase inverter monitoring", //
			50 /* length */, SolarEdgeSunSpecModel.S103.values(), SunSpecModelType.INVERTER); //
	
	public static enum S1 implements SunSpecPoint {
		MN(new ValuePoint("S1_MN", "Manufacturer", //
				"Well known value registered with SunSpec for compliance", //
				STRING16, true /* mandatory? */, READ_ONLY, Unit.NONE)), //
		MD(new ValuePoint("S1_MD", "Model", //
				"Manufacturer specific value (32 chars)", //
				STRING16, true /* mandatory? */, READ_ONLY, Unit.NONE)), //
		OPT(new ValuePoint("S1_OPT", "Options", //
				"Manufacturer specific value (16 chars)", //
				STRING8, false /* mandatory? */, READ_ONLY, Unit.NONE)), //
		VR(new ValuePoint("S1_VR", "Version", //
				"Manufacturer specific value (16 chars)", //
				STRING8, false /* mandatory? */, READ_ONLY, Unit.NONE)), //
		SN(new ValuePoint("S1_SN", "Serial Number", //
				"Manufacturer specific value (32 chars)", //
				STRING16, true /* mandatory? */, READ_ONLY, Unit.NONE)), //
		DA(new ValuePoint("S1_DA", "Device Address", //
				"Modbus device address", //
				UINT16, false /* mandatory? */, READ_WRITE, Unit.NONE)), //
		PAD(new ValuePoint("S1_PAD", "", //
				"Force even alignment", //
				ValuePoint.Type.PAD, false /* mandatory? */, READ_ONLY, Unit.NONE));

		private final Point point;

		private S1(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}
	
	public static enum S101 implements SunSpecPoint {
		A(new ScaledValuePoint("S101_A", "Amps", //
				"AC Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_A(new ScaledValuePoint("S101_APH_A", "Amps PhaseA", //
				"Phase A Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_B(new ScaledValuePoint("S101_APH_B", "Amps PhaseB", //
				"Phase B Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_C(new ScaledValuePoint("S101_APH_C", "Amps PhaseC", //
				"Phase C Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		A_SF(new ScaleFactorPoint("S101_A_SF", "", "")), //
		P_P_VPH_A_B(new ScaledValuePoint("S101_P_P_VPH_A_B", "Phase Voltage AB", //
				"Phase Voltage AB", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_B_C(new ScaledValuePoint("S101_P_P_VPH_B_C", "Phase Voltage BC", //
				"Phase Voltage BC", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_C_A(new ScaledValuePoint("S101_P_P_VPH_C_A", "Phase Voltage CA", //
				"Phase Voltage CA", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_A(new ScaledValuePoint("S101_PH_VPH_A", "Phase Voltage AN", //
				"Phase Voltage AN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_B(new ScaledValuePoint("S101_PH_VPH_B", "Phase Voltage BN", //
				"Phase Voltage BN", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_C(new ScaledValuePoint("S101_PH_VPH_C", "Phase Voltage CN", //
				"Phase Voltage CN", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		V_SF(new ScaleFactorPoint("S101_V_SF", "", "")), //
		W(new ScaledValuePoint("S101_W", "Watts", //
				"AC Power", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.WATT, "W_SF")), //
		W_SF(new ScaleFactorPoint("S101_W_SF", "", "")), //
		HZ(new ScaledValuePoint("S101_HZ", "Hz", //
				"Line Frequency", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.HERTZ, "Hz_SF")), //
		HZ_S_F(new ScaleFactorPoint("S101_HZ_S_F", "", "")), //
		VA(new ScaledValuePoint("S101_VA", "VA", //
				"AC Apparent Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE, "VA_SF")), //
		VA_SF(new ScaleFactorPoint("S101_VA_SF", "", "")), //
		V_AR(new ScaledValuePoint("S101_V_AR", "VAr", //
				"AC Reactive Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE_REACTIVE, "VAr_SF")), //
		V_AR_S_F(new ScaleFactorPoint("S101_V_AR_S_F", "", "")), //
		PF(new ScaledValuePoint("S101_PF", "PF", //
				"AC Power Factor", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.NONE, "PF_SF")), //
		PF_SF(new ScaleFactorPoint("S101_PF_SF", "", "")), //
		WH(new ScaledValuePoint("S101_WH", "WattHours", //
				"AC Energy", //
				ACC32, true /* mandatory? */, READ_ONLY, Unit.CUMULATED_WATT_HOURS, "WH_SF")), //
		WH_SF(new ScaleFactorPoint("S101_WH_SF", "", "")), //
		DCA(new ScaledValuePoint("S101_DCA", "DC Amps", //
				"DC Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "DCA_SF")), //
		DCA_SF(new ScaleFactorPoint("S101_DCA_SF", "", "")), //
		DCV(new ScaledValuePoint("S101_DCV", "DC Voltage", //
				"DC Voltage", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "DCV_SF")), //
		DCV_SF(new ScaleFactorPoint("S101_DCV_SF", "", "")), //
		DCW(new ScaledValuePoint("S101_DCW", "DC Watts", //
				"DC Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.WATT, "DCW_SF")), //
		DCW_SF(new ScaleFactorPoint("S101_DCW_SF", "", "")), //
		TMP_CAB(new ScaledValuePoint("S101_TMP_CAB", "Cabinet Temperature", //
				"Cabinet Temperature", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_SNK(new ScaledValuePoint("S101_TMP_SNK", "Heat Sink Temperature", //
				"Heat Sink Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_TRNS(new ScaledValuePoint("S101_TMP_TRNS", "Transformer Temperature", //
				"Transformer Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_OT(new ScaledValuePoint("S101_TMP_OT", "Other Temperature", //
				"Other Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_S_F(new ScaleFactorPoint("S101_TMP_S_F", "", "")), //
		ST(new EnumPoint("S101_ST", "Operating State", //
				"Enumerated value.  Operating state", //
				ENUM16, true /* mandatory? */, READ_ONLY, S101_St.values())), //
		ST_VND(new EnumPoint("S101_ST_VND", "Vendor Operating State", //
				"Vendor specific operating state code", //
				ENUM16, false /* mandatory? */, READ_ONLY, new OptionsEnum[0]));

		private final Point point;

		private S101(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}
	
	public static enum S101_St implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		OFF(1, "OFF"), //
		SLEEPING(2, "SLEEPING"), //
		STARTING(3, "STARTING"), //
		MPPT(4, "MPPT"), //
		THROTTLED(5, "THROTTLED"), //
		SHUTTING_DOWN(6, "SHUTTING_DOWN"), //
		FAULT(7, "FAULT"), //
		STANDBY(8, "STANDBY");

		private final int value;
		private final String name;

		private S101_St(int value, String name) {
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

	public static enum S102 implements SunSpecPoint {
		A(new ScaledValuePoint("S102_A", "Amps", //
				"AC Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_A(new ScaledValuePoint("S102_APH_A", "Amps PhaseA", //
				"Phase A Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_B(new ScaledValuePoint("S102_APH_B", "Amps PhaseB", //
				"Phase B Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_C(new ScaledValuePoint("S102_APH_C", "Amps PhaseC", //
				"Phase C Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		A_SF(new ScaleFactorPoint("S102_A_SF", "", "")), //
		P_P_VPH_A_B(new ScaledValuePoint("S102_P_P_VPH_A_B", "Phase Voltage AB", //
				"Phase Voltage AB", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_B_C(new ScaledValuePoint("S102_P_P_VPH_B_C", "Phase Voltage BC", //
				"Phase Voltage BC", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_C_A(new ScaledValuePoint("S102_P_P_VPH_C_A", "Phase Voltage CA", //
				"Phase Voltage CA", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_A(new ScaledValuePoint("S102_PH_VPH_A", "Phase Voltage AN", //
				"Phase Voltage AN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_B(new ScaledValuePoint("S102_PH_VPH_B", "Phase Voltage BN", //
				"Phase Voltage BN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_C(new ScaledValuePoint("S102_PH_VPH_C", "Phase Voltage CN", //
				"Phase Voltage CN", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		V_SF(new ScaleFactorPoint("S102_V_SF", "", "")), //
		W(new ScaledValuePoint("S102_W", "Watts", //
				"AC Power", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.WATT, "W_SF")), //
		W_SF(new ScaleFactorPoint("S102_W_SF", "", "")), //
		HZ(new ScaledValuePoint("S102_HZ", "Hz", //
				"Line Frequency", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.HERTZ, "Hz_SF")), //
		HZ_S_F(new ScaleFactorPoint("S102_HZ_S_F", "", "")), //
		VA(new ScaledValuePoint("S102_VA", "VA", //
				"AC Apparent Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE, "VA_SF")), //
		VA_SF(new ScaleFactorPoint("S102_VA_SF", "", "")), //
		V_AR(new ScaledValuePoint("S102_V_AR", "VAr", //
				"AC Reactive Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE_REACTIVE, "VAr_SF")), //
		V_AR_S_F(new ScaleFactorPoint("S102_V_AR_S_F", "", "")), //
		PF(new ScaledValuePoint("S102_PF", "PF", //
				"AC Power Factor", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.NONE, "PF_SF")), //
		PF_SF(new ScaleFactorPoint("S102_PF_SF", "", "")), //
		WH(new ScaledValuePoint("S102_WH", "WattHours", //
				"AC Energy", //
				ACC32, true /* mandatory? */, READ_ONLY, Unit.CUMULATED_WATT_HOURS, "WH_SF")), //
		WH_SF(new ScaleFactorPoint("S102_WH_SF", "", "")), //
		DCA(new ScaledValuePoint("S102_DCA", "DC Amps", //
				"DC Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "DCA_SF")), //
		DCA_SF(new ScaleFactorPoint("S102_DCA_SF", "", "")), //
		DCV(new ScaledValuePoint("S102_DCV", "DC Voltage", //
				"DC Voltage", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "DCV_SF")), //
		DCV_SF(new ScaleFactorPoint("S102_DCV_SF", "", "")), //
		DCW(new ScaledValuePoint("S102_DCW", "DC Watts", //
				"DC Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.WATT, "DCW_SF")), //
		DCW_SF(new ScaleFactorPoint("S102_DCW_SF", "", "")), //
		TMP_CAB(new ScaledValuePoint("S102_TMP_CAB", "Cabinet Temperature", //
				"Cabinet Temperature", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_SNK(new ScaledValuePoint("S102_TMP_SNK", "Heat Sink Temperature", //
				"Heat Sink Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_TRNS(new ScaledValuePoint("S102_TMP_TRNS", "Transformer Temperature", //
				"Transformer Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_OT(new ScaledValuePoint("S102_TMP_OT", "Other Temperature", //
				"Other Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_S_F(new ScaleFactorPoint("S102_TMP_S_F", "", "")), //
		ST(new EnumPoint("S102_ST", "Operating State", //
				"Enumerated value.  Operating state", //
				ENUM16, true /* mandatory? */, READ_ONLY, S102_St.values())), //
		ST_VND(new EnumPoint("S102_ST_VND", "Vendor Operating State", //
				"Vendor specific operating state code", //
				ENUM16, false /* mandatory? */, READ_ONLY, new OptionsEnum[0])); //

		private final Point point;

		private S102(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}

	public static enum S102_St implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		OFF(1, "OFF"), //
		SLEEPING(2, "SLEEPING"), //
		STARTING(3, "STARTING"), //
		MPPT(4, "MPPT"), //
		THROTTLED(5, "THROTTLED"), //
		SHUTTING_DOWN(6, "SHUTTING_DOWN"), //
		FAULT(7, "FAULT"), //
		STANDBY(8, "STANDBY");

		private final int value;
		private final String name;

		private S102_St(int value, String name) {
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

	public static enum S103 implements SunSpecPoint {
		A(new ScaledValuePoint("S103_A", "Amps", //
				"AC Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_A(new ScaledValuePoint("S103_APH_A", "Amps PhaseA", //
				"Phase A Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_B(new ScaledValuePoint("S103_APH_B", "Amps PhaseB", //
				"Phase B Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		APH_C(new ScaledValuePoint("S103_APH_C", "Amps PhaseC", //
				"Phase C Current", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.AMPERE, "A_SF")), //
		A_SF(new ScaleFactorPoint("S103_A_SF", "", "")), //
		P_P_VPH_A_B(new ScaledValuePoint("S103_P_P_VPH_A_B", "Phase Voltage AB", //
				"Phase Voltage AB", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_B_C(new ScaledValuePoint("S103_P_P_VPH_B_C", "Phase Voltage BC", //
				"Phase Voltage BC", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		P_P_VPH_C_A(new ScaledValuePoint("S103_P_P_VPH_C_A", "Phase Voltage CA", //
				"Phase Voltage CA", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_A(new ScaledValuePoint("S103_PH_VPH_A", "Phase Voltage AN", //
				"Phase Voltage AN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_B(new ScaledValuePoint("S103_PH_VPH_B", "Phase Voltage BN", //
				"Phase Voltage BN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		PH_VPH_C(new ScaledValuePoint("S103_PH_VPH_C", "Phase Voltage CN", //
				"Phase Voltage CN", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.VOLT, "V_SF")), //
		V_SF(new ScaleFactorPoint("S103_V_SF", "", "")), //
		W(new ScaledValuePoint("S103_W", "Watts", //
				"AC Power", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.WATT, "W_SF")), //
		W_SF(new ScaleFactorPoint("S103_W_SF", "", "")), //
		HZ(new ScaledValuePoint("S103_HZ", "Hz", //
				"Line Frequency", //
				UINT16, true /* mandatory? */, READ_ONLY, Unit.HERTZ, "Hz_SF")), //
		HZ_S_F(new ScaleFactorPoint("S103_HZ_S_F", "", "")), //
		VA(new ScaledValuePoint("S103_VA", "VA", //
				"AC Apparent Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE, "VA_SF")), //
		VA_SF(new ScaleFactorPoint("S103_VA_SF", "", "")), //
		V_AR(new ScaledValuePoint("S103_V_AR", "VAr", //
				"AC Reactive Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.VOLT_AMPERE_REACTIVE, "VAr_SF")), //
		V_AR_S_F(new ScaleFactorPoint("S103_V_AR_S_F", "", "")), //
		PF(new ScaledValuePoint("S103_PF", "PF", //
				"AC Power Factor", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.NONE, "PF_SF")), //
		PF_SF(new ScaleFactorPoint("S103_PF_SF", "", "")), //
		WH(new ScaledValuePoint("S103_WH", "WattHours", //
				"AC Energy", //
				ACC32, true /* mandatory? */, READ_ONLY, Unit.CUMULATED_WATT_HOURS, "WH_SF")), //
		WH_SF(new ScaleFactorPoint("S103_WH_SF", "", "")), //
		DCA(new ScaledValuePoint("S103_DCA", "DC Amps", //
				"DC Current", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.AMPERE, "DCA_SF")), //
		DCA_SF(new ScaleFactorPoint("S103_DCA_SF", "", "")), //
		DCV(new ScaledValuePoint("S103_DCV", "DC Voltage", //
				"DC Voltage", //
				UINT16, false /* mandatory? */, READ_ONLY, Unit.VOLT, "DCV_SF")), //
		DCV_SF(new ScaleFactorPoint("S103_DCV_SF", "", "")), //
		DCW(new ScaledValuePoint("S103_DCW", "DC Watts", //
				"DC Power", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.WATT, "DCW_SF")), //
		DCW_SF(new ScaleFactorPoint("S103_DCW_SF", "", "")), //
		TMP_CAB(new ScaledValuePoint("S103_TMP_CAB", "Cabinet Temperature", //
				"Cabinet Temperature", //
				INT16, true /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_SNK(new ScaledValuePoint("S103_TMP_SNK", "Heat Sink Temperature", //
				"Heat Sink Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_TRNS(new ScaledValuePoint("S103_TMP_TRNS", "Transformer Temperature", //
				"Transformer Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_OT(new ScaledValuePoint("S103_TMP_OT", "Other Temperature", //
				"Other Temperature", //
				INT16, false /* mandatory? */, READ_ONLY, Unit.DEGREE_CELSIUS, "Tmp_SF")), //
		TMP_S_F(new ScaleFactorPoint("S103_TMP_S_F", "", "")), //
		ST(new EnumPoint("S103_ST", "Operating State", //
				"Enumerated value.  Operating state", //
				ENUM16, true /* mandatory? */, READ_ONLY, S103_St.values())), //
		ST_VND(new EnumPoint("S103_ST_VND", "Vendor Operating State", //
				"Vendor specific operating state code", //
				ENUM16, false /* mandatory? */, READ_ONLY, new OptionsEnum[0])); //

		private final Point point;

		private S103(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}

	public static enum S103_St implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		OFF(1, "OFF"), //
		SLEEPING(2, "SLEEPING"), //
		STARTING(3, "STARTING"), //
		MPPT(4, "MPPT"), //
		THROTTLED(5, "THROTTLED"), //
		SHUTTING_DOWN(6, "SHUTTING_DOWN"), //
		FAULT(7, "FAULT"), //
		STANDBY(8, "STANDBY");

		private final int value;
		private final String name;

		private S103_St(int value, String name) {
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
	
	public final String label;
	public final String description;
	public final int length;
	public final SunSpecPoint[] points;
	public final SunSpecModelType modelType;

	private SolarEdgeSunSpecModel(String label, String description, int length, SunSpecPoint[] points,
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
}