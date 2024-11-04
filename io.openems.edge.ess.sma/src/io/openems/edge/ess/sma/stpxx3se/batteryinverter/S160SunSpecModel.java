// CHECKSTYLE:OFF

package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.Point;
import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint.SunSpecBitPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.BitPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;

//TODO same as FroniusSunspecModel - merge together 
public enum S160SunSpecModel implements SunSpecModel {
	S_160(//
			"Mutliple MPPT Inverter Extension Model", //
			"Mutliple MPPT Inverter Extension Model", //
			"", //
			88, //
			S160SunSpecModel.S160.values(), //
			SunSpecModelType.INVERTER //
	); //
	
	public static enum S160 implements SunSpecPoint {
		DCA_SF(new ScaleFactorPoint(//
				"S160_DCA_S_F", //
				"Dca_SF", //
				"")), //
		DCV_SF(new ScaleFactorPoint(//
				"S160_DCV_S_F", //
				"Dcv_SF", //
				"")), //
		DCW_SF(new ScaleFactorPoint(//
				"S160_DCW_S_F", //
				"Dcw_SF", //
				"")), //
		DCWH_SF(new ScaleFactorPoint(//
				"S160_DCWH_S_F", //
				"Dcwh_SF", //
				"")), //
		EVT(new BitFieldPoint(//
				"S160_EVT", //
				"Evt", //
				"Global Events", //
				BitFieldPoint.Type.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				S160_Evt.values())), //
		N(new ValuePoint(//
				"S160_N", //
				"N", //
				"Number of Modules", //
				ValuePoint.Type.COUNT, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //
		TMS_PER(new ValuePoint(//
				"S160_TMS_PER", //
				"TmsPer", //
				"Timestamp Period", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	

		
		
		MODULE_1_I_D(new ValuePoint(//
				"S160_MODULE_1_I_D", //
				"Module1Id", //
				"Input ID", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_1_I_D_STR(new ValuePoint(//
				"S160_MODULE_1_I_D_STR", //
				"Module1IdStr", //
				"Input ID String", //
				ValuePoint.Type.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_1_D_C_A(new ScaledValuePoint(//
				"S160_MODULE_1_D_C_A", //
				"Module1Dca", //
				"DC Current", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF")), //	
		MODULE_1_D_C_V(new ScaledValuePoint(//
				"S160_MODULE_1_D_C_V", //
				"Module1Dcv", //
				"DC Voltage", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF")), //	
		MODULE_1_D_C_W(new ScaledValuePoint(//
				"S160_MODULE_1_D_C_W", //
				"Module1Dcw", //
				"DC Power", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF")), //	
		MODULE_1_D_C_W_H(new ScaledValuePoint(//
				"S160_MODULE_1_D_C_W_H", //
				"Module1Dcwh", //
				"Lifetime Energy", //
				ValuePoint.Type.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF")), //	
		MODULE_1_TMS(new ValuePoint(//
				"S160_MODULE_1_TMS", //
				"Module1Timestamp", //
				"Timestamp", //
				ValuePoint.Type.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS)), //	
		MODULE_1_TMP(new ValuePoint(//
				"S160_MODULE_1_TEMP", //
				"Module1Temp", //
				"Temperature", //
				ValuePoint.Type.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS)), //	
		MODULE_1_D_C_ST(new EnumPoint(//
				"S160_MODULE_1_OP_STATES", //
				"Module1OpStates", //
				"Operating State", //
				EnumPoint.Type.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				S160_DCSt.values())), //	
		MODULE_1_EVT(new BitFieldPoint(//
				"S160_MODULE_1_EVT", //
				"Evt", //
				"Module Events", //
				BitFieldPoint.Type.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				S160_Module_1_Evt.values())), //	
		
		MODULE_2_I_D(new ValuePoint(//
				"S160_MODULE_2_I_D", //
				"Module2Id", //
				"Input ID", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_2_I_D_STR(new ValuePoint(//
				"S160_MODULE_2_I_D_STR", //
				"Module2IdStr", //
				"Input ID String", //
				ValuePoint.Type.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_2_D_C_A(new ScaledValuePoint(//
				"S160_MODULE_2_D_C_A", //
				"Module2Dca", //
				"DC Current", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF")), //	
		MODULE_2_D_C_V(new ScaledValuePoint(//
				"S160_MODULE_2_D_C_V", //
				"Module2Dcv", //
				"DC Voltage", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF")), //	
		MODULE_2_D_C_W(new ScaledValuePoint(//
				"S160_MODULE_2_D_C_W", //
				"Module2Dcw", //
				"DC Power", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF")), //	
		MODULE_2_D_C_W_H(new ScaledValuePoint(//
				"S160_MODULE_2_D_C_W_H", //
				"Module2Dcwh", //
				"Lifetime Energy", //
				ValuePoint.Type.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF")), //	
		MODULE_2_TMS(new ValuePoint(//
				"S160_MODULE_2_TMS", //
				"Module2Timestamp", //
				"Timestamp", //
				ValuePoint.Type.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS)), //	
		MODULE_2_TMP(new ValuePoint(//
				"S160_MODULE_2_TEMP", //
				"Module2Temp", //
				"Temperature", //
				ValuePoint.Type.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS)), //	
		MODULE_2_D_C_ST(new EnumPoint(//
				"S160_MODULE_2_OP_STATES", //
				"Module2OpStates", //
				"Operating State", //
				EnumPoint.Type.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				S160_DCSt.values())), //	
		MODULE_2_EVT(new BitFieldPoint(//
				"S160_MODULE_2_EVT", //
				"Evt", //
				"Module Events", //
				BitFieldPoint.Type.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				S160_Module_2_Evt.values())), //	
		
		
		MODULE_3_I_D(new ValuePoint(//
				"S160_MODULE_3_I_D", //
				"Module2Id", //
				"Input ID", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_3_I_D_STR(new ValuePoint(//
				"S160_MODULE_3_I_D_STR", //
				"Module3IdStr", //
				"Input ID String", //
				ValuePoint.Type.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_3_D_C_A(new ScaledValuePoint(//
				"S160_MODULE_3_D_C_A", //
				"Module3Dca", //
				"DC Current", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF")), //	
		MODULE_3_D_C_V(new ScaledValuePoint(//
				"S160_MODULE_3_D_C_V", //
				"Module3Dcv", //
				"DC Voltage", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF")), //	
		MODULE_3_D_C_W(new ScaledValuePoint(//
				"S160_MODULE_3_D_C_W", //
				"Module3Dcw", //
				"DC Power", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF")), //	
		MODULE_3_D_C_W_H(new ScaledValuePoint(//
				"S160_MODULE_3_D_C_W_H", //
				"Module3Dcwh", //
				"Lifetime Energy", //
				ValuePoint.Type.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF")), //	
		MODULE_3_TMS(new ValuePoint(//
				"S160_MODULE_3_TMS", //
				"Module3Timestamp", //
				"Timestamp", //
				ValuePoint.Type.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS)), //	
		MODULE_3_TMP(new ValuePoint(//
				"S160_MODULE_3_TEMP", //
				"Module3Temp", //
				"Temperature", //
				ValuePoint.Type.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS)), //	
		MODULE_3_D_C_ST(new EnumPoint(//
				"S160_MODULE_3_OP_STATES", //
				"Module3OpStates", //
				"Operating State", //
				EnumPoint.Type.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				S160_DCSt.values())), //	
		MODULE_3_EVT(new BitFieldPoint(//
				"S160_MODULE_3_EVT", //
				"Evt", //
				"Module Events", //
				BitFieldPoint.Type.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				S160_Module_3_Evt.values())), //	
		
		
		MODULE_4_I_D(new ValuePoint(//
				"S160_MODULE_4_I_D", //
				"Module4Id", //
				"Input ID", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_4_I_D_STR(new ValuePoint(//
				"S160_MODULE_4_I_D_STR", //
				"Module4IdStr", //
				"Input ID String", //
				ValuePoint.Type.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE)), //	
		MODULE_4_D_C_A(new ScaledValuePoint(//
				"S160_MODULE_4_D_C_A", //
				"Module4Dca", //
				"DC Current", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF")), //	
		MODULE_4_D_C_V(new ScaledValuePoint(//
				"S160_MODULE_4_D_C_V", //
				"Module4Dcv", //
				"DC Voltage", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF")), //	
		MODULE_4_D_C_W(new ScaledValuePoint(//
				"S160_MODULE_4_D_C_W", //
				"Module4Dcw", //
				"DC Power", //
				ValuePoint.Type.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF")), //	
		MODULE_4_D_C_W_H(new ScaledValuePoint(//
				"S160_MODULE_4_D_C_W_H", //
				"Module4Dcwh", //
				"Lifetime Energy", //
				ValuePoint.Type.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF")), //	
		MODULE_4_TMS(new ValuePoint(//
				"S160_MODULE_4_TMS", //
				"Module4Timestamp", //
				"Timestamp", //
				ValuePoint.Type.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS)), //	
		MODULE_4_TMP(new ValuePoint(//
				"S160_MODULE_4_TEMP", //
				"Module4Temp", //
				"Temperature", //
				ValuePoint.Type.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS)), //	
		MODULE_4_D_C_ST(new EnumPoint(//
				"S160_MODULE_4_OP_STATES", //
				"Module4OpStates", //
				"Operating State", //
				EnumPoint.Type.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				S160_DCSt.values())), //	
		MODULE_4_EVT(new BitFieldPoint(//
				"S160_MODULE_4_EVT", //
				"Evt", //
				"Module Events", //
				BitFieldPoint.Type.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				S160_Module_4_Evt.values())), //	
		
		
		; //

		protected final Point point;

		private S160(Point point) {
			this.point = point;
		}

		@Override
		public Point get() {
			return this.point;
		}
	}
	
	public static enum S160_DCSt implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		OFF(1, "Off"), //
		SLEEPING(2, "Sleeping"), //
		STARTING(3, "Starting"), //
		MPPT(4, "MPPT"), //
		THROTTLED(5, "Throttled"), //
		SHUTTING_DOWN(6, "Shutting down"), //
		FAULT(7, "Fault"), //
		STANDBY(8, "Standby"), //
		TEST(9, "Test"), //
		RESERVED_10(10, "Reserved 10"); //
		
		private final int value;
		private final String name;
		
		private S160_DCSt(int value, String name) {
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
	
	public static enum S160_Evt implements SunSpecBitPoint {
		GROUND_FAULT(new BitPoint(0, "S160_EVT_GROUND_FAULT", "Ground Fault")), //
		INPUT_OVER_VOLTAGE(new BitPoint(1, "S160_EVT_INPUT_OVER_VOLTAGE", "Input over voltage")), //
		RESERVED_2(new BitPoint(2, "S160_EVT_RESERVED_2", "Reserved 2")), //
		DC_DISCONNECT(new BitPoint(3, "S160_EVT_D_C_DISCONNECT", "DC Disconnect")), //
		RESERVED_4(new BitPoint(4, "S160_EVT_RESERVED_4", "Reserved 4")), //
		CABINET_OPEN(new BitPoint(5, "S160_EVT_CABINET_OPEN", "Cabinet open")), //
		MANUAL_SHUTDOWN(new BitPoint(6, "S160_EVT_MANUAL_SHUTDOWN", "Manual Shutdown")), //
		OVER_TEMP(new BitPoint(7, "S160_EVT_OVER_TEMP", "Over Temp, ")), //
		RESERVED_8(new BitPoint(8, "S160_EVT_RESERVED_8", "Reserved 8")), //
		RESERVED_9(new BitPoint(9, "S160_EVT_RESERVED_9", "Reserved 9")), //
		RESERVED_10(new BitPoint(10, "S160_EVT_RESERVED_10", "Reserved 10")), //
		RESERVED_11(new BitPoint(11, "S160_EVT_RESERVED_11", "Reserved 11")), //
		BLOWN_FUSE(new BitPoint(12, "S160_EVT_BLOWN_FUSE", "Blown Fuse")), //
		UNDER_TEMP(new BitPoint(13, "S160_EVT_UNDER_TEMP", "Under Temp")), //
		MEMORY_LOSS(new BitPoint(14, "S160_EVT_MEMORY_LOSS", "Memory Loss")), //
		ARC_DETECTION(new BitPoint(15, "S160_EVT_ARC_DETECTION", "Arc Detection")), //
		RESERVED_16(new BitPoint(16, "S160_EVT_RESERVED_16", "Reserved 16")), //
		RESERVED_17(new BitPoint(17, "S160_EVT_RESERVED_17", "Reserved 17")), //
		RESERVED_18(new BitPoint(18, "S160_EVT_RESERVED_18", "Reserved 18")), //
		RESERVED_19(new BitPoint(19, "S160_EVT_RESERVED_19", "Reserved 19")), //
		TEST_FAILED(new BitPoint(20, "S160_EVT_TEST_FAILED", "Test failed")), //
		INPUT_UNDER_VOLTAGE(new BitPoint(21, "S160_EVT_INPUT_UNDER_VOLTAGE", "Input under voltage")), //
		INPUT_OVER_CURRENT(new BitPoint(22, "S160_EVT_INPUT_OVER_CURRENT", "Input over current")); //

		
		private final BitPoint point;

		private S160_Evt(BitPoint point) {
			this.point = point;
		}

		@Override
		public BitPoint get() {
			return this.point;
		}
	}
	
	public static enum S160_Module_1_Evt implements SunSpecBitPoint {
		GROUND_FAULT(new BitPoint(0, "S160_MODULE_1_EVT_GROUND_FAULT", "Ground Fault")), //
		INPUT_OVER_VOLTAGE(new BitPoint(1, "S160_MODULE_1_EVT_INPUT_OVER_VOLTAGE", "Input over voltage")), //
		RESERVED_2(new BitPoint(2, "S160_MODULE_1_EVT_RESERVED_2", "Reserved 2")), //
		DC_DISCONNECT(new BitPoint(3, "S160_MODULE_1_EVT_D_C_DISCONNECT", "DC Disconnect")), //
		RESERVED_4(new BitPoint(4, "S160_MODULE_1_EVT_RESERVED_4", "Reserved 4")), //
		CABINET_OPEN(new BitPoint(5, "S160_MODULE_1_EVT_CABINET_OPEN", "Cabinet open")), //
		MANUAL_SHUTDOWN(new BitPoint(6, "S160_MODULE_1_EVT_MANUAL_SHUTDOWN", "Manual Shutdown")), //
		OVER_TEMP(new BitPoint(7, "S160_MODULE_1_EVT_OVER_TEMP", "Over Temp, ")), //
		RESERVED_8(new BitPoint(8, "S160_MODULE_1_EVT_RESERVED_8", "Reserved 8")), //
		RESERVED_9(new BitPoint(9, "S160_MODULE_1_EVT_RESERVED_9", "Reserved 9")), //
		RESERVED_10(new BitPoint(10, "S160_MODULE_1_EVT_RESERVED_10", "Reserved 10")), //
		RESERVED_11(new BitPoint(11, "S160_MODULE_1_EVT_RESERVED_11", "Reserved 11")), //
		BLOWN_FUSE(new BitPoint(12, "S160_MODULE_1_EVT_BLOWN_FUSE", "Blown Fuse")), //
		UNDER_TEMP(new BitPoint(13, "S160_MODULE_1_EVT_UNDER_TEMP", "Under Temp")), //
		MEMORY_LOSS(new BitPoint(14, "S160_MODULE_1_EVT_MEMORY_LOSS", "Memory Loss")), //
		ARC_DETECTION(new BitPoint(15, "S160_MODULE_1_EVT_ARC_DETECTION", "Arc Detection")), //
		RESERVED_16(new BitPoint(16, "S160_MODULE_1_EVT_RESERVED_16", "Reserved 16")), //
		RESERVED_17(new BitPoint(17, "S160_MODULE_1_EVT_RESERVED_17", "Reserved 17")), //
		RESERVED_18(new BitPoint(18, "S160_MODULE_1_EVT_RESERVED_18", "Reserved 18")), //
		RESERVED_19(new BitPoint(19, "S160_MODULE_1_EVT_RESERVED_19", "Reserved 19")), //
		TEST_FAILED(new BitPoint(20, "S160_MODULE_1_EVT_TEST_FAILED", "Test failed")), //
		INPUT_UNDER_VOLTAGE(new BitPoint(21, "S160_MODULE_1_EVT_INPUT_UNDER_VOLTAGE", "Input under voltage")), //
		INPUT_OVER_CURRENT(new BitPoint(22, "S160_MODULE_1_EVT_INPUT_OVER_CURRENT", "Input over current")); //

		
		private final BitPoint point;

		private S160_Module_1_Evt(BitPoint point) {
			this.point = point;
		}

		@Override
		public BitPoint get() {
			return this.point;
		}
	}
	
	public static enum S160_Module_2_Evt implements SunSpecBitPoint {
		GROUND_FAULT(new BitPoint(0, "S160_MODULE_2_EVT_GROUND_FAULT", "Ground Fault")), //
		INPUT_OVER_VOLTAGE(new BitPoint(1, "S160_MODULE_2_EVT_INPUT_OVER_VOLTAGE", "Input over voltage")), //
		RESERVED_2(new BitPoint(2, "S160_MODULE_2_EVT_RESERVED_2", "Reserved 2")), //
		DC_DISCONNECT(new BitPoint(3, "S160_MODULE_2_EVT_D_C_DISCONNECT", "DC Disconnect")), //
		RESERVED_4(new BitPoint(4, "S160_MODULE_2_EVT_RESERVED_4", "Reserved 4")), //
		CABINET_OPEN(new BitPoint(5, "S160_MODULE_2_EVT_CABINET_OPEN", "Cabinet open")), //
		MANUAL_SHUTDOWN(new BitPoint(6, "S160_MODULE_2_EVT_MANUAL_SHUTDOWN", "Manual Shutdown")), //
		OVER_TEMP(new BitPoint(7, "S160_MODULE_2_EVT_OVER_TEMP", "Over Temp, ")), //
		RESERVED_8(new BitPoint(8, "S160_MODULE_2_EVT_RESERVED_8", "Reserved 8")), //
		RESERVED_9(new BitPoint(9, "S160_MODULE_2_EVT_RESERVED_9", "Reserved 9")), //
		RESERVED_10(new BitPoint(10, "S160_MODULE_2_EVT_RESERVED_10", "Reserved 10")), //
		RESERVED_11(new BitPoint(11, "S160_MODULE_2_EVT_RESERVED_11", "Reserved 11")), //
		BLOWN_FUSE(new BitPoint(12, "S160_MODULE_2_EVT_BLOWN_FUSE", "Blown Fuse")), //
		UNDER_TEMP(new BitPoint(13, "S160_MODULE_2_EVT_UNDER_TEMP", "Under Temp")), //
		MEMORY_LOSS(new BitPoint(14, "S160_MODULE_2_EVT_MEMORY_LOSS", "Memory Loss")), //
		ARC_DETECTION(new BitPoint(15, "S160_MODULE_2_EVT_ARC_DETECTION", "Arc Detection")), //
		RESERVED_16(new BitPoint(16, "S160_MODULE_2_EVT_RESERVED_16", "Reserved 16")), //
		RESERVED_17(new BitPoint(17, "S160_MODULE_2_EVT_RESERVED_17", "Reserved 17")), //
		RESERVED_18(new BitPoint(18, "S160_MODULE_2_EVT_RESERVED_18", "Reserved 18")), //
		RESERVED_19(new BitPoint(19, "S160_MODULE_2_EVT_RESERVED_19", "Reserved 19")), //
		TEST_FAILED(new BitPoint(20, "S160_MODULE_2_EVT_TEST_FAILED", "Test failed")), //
		INPUT_UNDER_VOLTAGE(new BitPoint(21, "S160_MODULE_2_EVT_INPUT_UNDER_VOLTAGE", "Input under voltage")), //
		INPUT_OVER_CURRENT(new BitPoint(22, "S160_MODULE_2_EVT_INPUT_OVER_CURRENT", "Input over current")); //

		
		private final BitPoint point;

		private S160_Module_2_Evt(BitPoint point) {
			this.point = point;
		}

		@Override
		public BitPoint get() {
			return this.point;
		}
	}
	
	public static enum S160_Module_3_Evt implements SunSpecBitPoint {
		GROUND_FAULT(new BitPoint(0, "S160_MODULE_3_EVT_GROUND_FAULT", "Ground Fault")), //
		INPUT_OVER_VOLTAGE(new BitPoint(1, "S160_MODULE_3_EVT_INPUT_OVER_VOLTAGE", "Input over voltage")), //
		RESERVED_2(new BitPoint(2, "S160_MODULE_3_EVT_RESERVED_2", "Reserved 2")), //
		DC_DISCONNECT(new BitPoint(3, "S160_MODULE_3_EVT_D_C_DISCONNECT", "DC Disconnect")), //
		RESERVED_4(new BitPoint(4, "S160_MODULE_3_EVT_RESERVED_4", "Reserved 4")), //
		CABINET_OPEN(new BitPoint(5, "S160_MODULE_3_EVT_CABINET_OPEN", "Cabinet open")), //
		MANUAL_SHUTDOWN(new BitPoint(6, "S160_MODULE_3_EVT_MANUAL_SHUTDOWN", "Manual Shutdown")), //
		OVER_TEMP(new BitPoint(7, "S160_MODULE_3_EVT_OVER_TEMP", "Over Temp, ")), //
		RESERVED_8(new BitPoint(8, "S160_MODULE_3_EVT_RESERVED_8", "Reserved 8")), //
		RESERVED_9(new BitPoint(9, "S160_MODULE_3_EVT_RESERVED_9", "Reserved 9")), //
		RESERVED_10(new BitPoint(10, "S160_MODULE_3_EVT_RESERVED_10", "Reserved 10")), //
		RESERVED_11(new BitPoint(11, "S160_MODULE_3_EVT_RESERVED_11", "Reserved 11")), //
		BLOWN_FUSE(new BitPoint(12, "S160_MODULE_3_EVT_BLOWN_FUSE", "Blown Fuse")), //
		UNDER_TEMP(new BitPoint(13, "S160_MODULE_3_EVT_UNDER_TEMP", "Under Temp")), //
		MEMORY_LOSS(new BitPoint(14, "S160_MODULE_3_EVT_MEMORY_LOSS", "Memory Loss")), //
		ARC_DETECTION(new BitPoint(15, "S160_MODULE_3_EVT_ARC_DETECTION", "Arc Detection")), //
		RESERVED_16(new BitPoint(16, "S160_MODULE_3_EVT_RESERVED_16", "Reserved 16")), //
		RESERVED_17(new BitPoint(17, "S160_MODULE_3_EVT_RESERVED_17", "Reserved 17")), //
		RESERVED_18(new BitPoint(18, "S160_MODULE_3_EVT_RESERVED_18", "Reserved 18")), //
		RESERVED_19(new BitPoint(19, "S160_MODULE_3_EVT_RESERVED_19", "Reserved 19")), //
		TEST_FAILED(new BitPoint(20, "S160_MODULE_3_EVT_TEST_FAILED", "Test failed")), //
		INPUT_UNDER_VOLTAGE(new BitPoint(21, "S160_MODULE_3_EVT_INPUT_UNDER_VOLTAGE", "Input under voltage")), //
		INPUT_OVER_CURRENT(new BitPoint(22, "S160_MODULE_3_EVT_INPUT_OVER_CURRENT", "Input over current")); //

		
		private final BitPoint point;

		private S160_Module_3_Evt(BitPoint point) {
			this.point = point;
		}

		@Override
		public BitPoint get() {
			return this.point;
		}
	}
	
	public static enum S160_Module_4_Evt implements SunSpecBitPoint {
		GROUND_FAULT(new BitPoint(0, "S160_MODULE_4_EVT_GROUND_FAULT", "Ground Fault")), //
		INPUT_OVER_VOLTAGE(new BitPoint(1, "S160_MODULE_4_EVT_INPUT_OVER_VOLTAGE", "Input over voltage")), //
		RESERVED_2(new BitPoint(2, "S160_MODULE_4_EVT_RESERVED_2", "Reserved 2")), //
		DC_DISCONNECT(new BitPoint(3, "S160_MODULE_4_EVT_D_C_DISCONNECT", "DC Disconnect")), //
		RESERVED_4(new BitPoint(4, "S160_MODULE_4_EVT_RESERVED_4", "Reserved 4")), //
		CABINET_OPEN(new BitPoint(5, "S160_MODULE_4_EVT_CABINET_OPEN", "Cabinet open")), //
		MANUAL_SHUTDOWN(new BitPoint(6, "S160_MODULE_4_EVT_MANUAL_SHUTDOWN", "Manual Shutdown")), //
		OVER_TEMP(new BitPoint(7, "S160_MODULE_4_EVT_OVER_TEMP", "Over Temp, ")), //
		RESERVED_8(new BitPoint(8, "S160_MODULE_4_EVT_RESERVED_8", "Reserved 8")), //
		RESERVED_9(new BitPoint(9, "S160_MODULE_4_EVT_RESERVED_9", "Reserved 9")), //
		RESERVED_10(new BitPoint(10, "S160_MODULE_4_EVT_RESERVED_10", "Reserved 10")), //
		RESERVED_11(new BitPoint(11, "S160_MODULE_4_EVT_RESERVED_11", "Reserved 11")), //
		BLOWN_FUSE(new BitPoint(12, "S160_MODULE_4_EVT_BLOWN_FUSE", "Blown Fuse")), //
		UNDER_TEMP(new BitPoint(13, "S160_MODULE_4_EVT_UNDER_TEMP", "Under Temp")), //
		MEMORY_LOSS(new BitPoint(14, "S160_MODULE_4_EVT_MEMORY_LOSS", "Memory Loss")), //
		ARC_DETECTION(new BitPoint(15, "S160_MODULE_4_EVT_ARC_DETECTION", "Arc Detection")), //
		RESERVED_16(new BitPoint(16, "S160_MODULE_4_EVT_RESERVED_16", "Reserved 16")), //
		RESERVED_17(new BitPoint(17, "S160_MODULE_4_EVT_RESERVED_17", "Reserved 17")), //
		RESERVED_18(new BitPoint(18, "S160_MODULE_4_EVT_RESERVED_18", "Reserved 18")), //
		RESERVED_19(new BitPoint(19, "S160_MODULE_4_EVT_RESERVED_19", "Reserved 19")), //
		TEST_FAILED(new BitPoint(20, "S160_MODULE_4_EVT_TEST_FAILED", "Test failed")), //
		INPUT_UNDER_VOLTAGE(new BitPoint(21, "S160_MODULE_4_EVT_INPUT_UNDER_VOLTAGE", "Input under voltage")), //
		INPUT_OVER_CURRENT(new BitPoint(22, "S160_MODULE_4_EVT_INPUT_OVER_CURRENT", "Input over current")); //

		
		private final BitPoint point;

		private S160_Module_4_Evt(BitPoint point) {
			this.point = point;
		}

		@Override
		public BitPoint get() {
			return this.point;
		}
	}
	
	public final String label;
	public final String description;
	public final String notes;
	public final int length;
	public final SunSpecPoint[] points;
	public final SunSpecModelType modelType;
	
	private S160SunSpecModel(String label, String description, String notes, int length, SunSpecPoint[] points,
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

}

//CHECKSTYLE:ON
