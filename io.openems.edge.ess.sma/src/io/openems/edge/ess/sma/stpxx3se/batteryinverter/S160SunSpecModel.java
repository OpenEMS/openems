// CHECKSTYLE:OFF

package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
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
		DCA_SF(new PointImpl(//
				"S160_DCA_S_F", //
				"Dca_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		DCV_SF(new PointImpl(//
				"S160_DCV_S_F", //
				"Dcv_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		DCW_SF(new PointImpl(//
				"S160_DCW_S_F", //
				"Dcw_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		DCWH_SF(new PointImpl(//
				"S160_DCWH_S_F", //
				"Dcwh_SF", //
				"", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		EVT(new PointImpl(//
				"S160_EVT", //
				"Evt", //
				"Global Events", //
				"", //
				PointType.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		N(new PointImpl(//
				"S160_N", //
				"N", //
				"Number of Modules", //
				"", //
				PointType.COUNT, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		TMS_PER(new PointImpl(//
				"S160_TMS_PER", //
				"TmsPer", //
				"TmsPer", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	

		
		
		MODULE_1_I_D(new PointImpl(//
				"S160_MODULE_1_I_D", //
				"Module1Id", //
				"Module1Id", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_1_I_D_STR(new PointImpl(//
				"S160_MODULE_1_I_D_STR", //
				"Module1IdStr", //
				"Module1IdStr", //
				"", //
				PointType.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_1_D_C_A(new PointImpl(//
				"S160_MODULE_1_D_C_A", //
				"Module1Dca", //
				"Module1Dca", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF", //
				new OptionsEnum[0])), //	
		MODULE_1_D_C_V(new PointImpl(//
				"S160_MODULE_1_D_C_V", //
				"Module1Dcv", //
				"Module1Dcv", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF", //
				new OptionsEnum[0])), //	
		MODULE_1_D_C_W(new PointImpl(//
				"S160_MODULE_1_D_C_W", //
				"Module1Dcw", //
				"Module1Dcw", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF", //
				new OptionsEnum[0])), //	
		MODULE_1_D_C_W_H(new PointImpl(//
				"S160_MODULE_1_D_C_W_H", //
				"Module1Dcwh", //
				"Module1Dcwh", //
				"", //
				PointType.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF", //
				new OptionsEnum[0])), //	
		MODULE_1_TMS(new PointImpl(//
				"S160_MODULE_1_TMS", //
				"Module1Timestamp", //
				"Module1Tms", //
				"", //
				PointType.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_1_TMP(new PointImpl(//
				"S160_MODULE_1_TEMP", //
				"Module1Temp", //
				"Module1Temp", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_1_D_C_ST(new PointImpl(//
				"S160_MODULE_1_OP_STATES", //
				"Module1OpStates", //
				"Module1OpStates", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCSt.values())), //	
		MODULE_1_EVT(new PointImpl(//
				"S160_MODULE_1_EVENTS", //
				"Module1Events", //
				"Module1Events", //
				"", //
				PointType.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCEvt.values())), //	
		

		
		
		MODULE_2_I_D(new PointImpl(//
				"S160_MODULE_2_I_D", //
				"Module2Id", //
				"Module2Id", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_2_I_D_STR(new PointImpl(//
				"S160_MODULE_2_I_D_STR", //
				"Module2IdStr", //
				"Module2IdStr", //
				"", //
				PointType.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_2_D_C_A(new PointImpl(//
				"S160_MODULE_2_D_C_A", //
				"Module2Dca", //
				"Module2Dca", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF", //
				new OptionsEnum[0])), //	
		MODULE_2_D_C_V(new PointImpl(//
				"S160_MODULE_2_D_C_V", //
				"Module2Dcv", //
				"Module2Dcv", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF", //
				new OptionsEnum[0])), //	
		MODULE_2_D_C_W(new PointImpl(//
				"S160_MODULE_2_D_C_W", //
				"Module2Dcw", //
				"Module2Dcw", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF", //
				new OptionsEnum[0])), //	
		MODULE_2_D_C_W_H(new PointImpl(//
				"S160_MODULE_2_D_C_W_H", //
				"Module2Dcwh", //
				"Module2Dcwh", //
				"", //
				PointType.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF", //
				new OptionsEnum[0])), //	
		MODULE_2_TMS(new PointImpl(//
				"S160_MODULE_2_TMS", //
				"Module2Tms", //
				"Module2Tms", //
				"", //
				PointType.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_2_TMP(new PointImpl(//
				"S160_MODULE_2_TEMP", //
				"Module2Temp", //
				"Module2Temp", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_2_D_C_ST(new PointImpl(//
				"S160_MODULE_2_OP_STATES", //
				"Module2OpStates", //
				"Module2OpStates", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCSt.values())), //	
		MODULE_2_EVT(new PointImpl(//
				"S160_MODULE_2_EVENTS", //
				"Module2Events", //
				"Module2Events", //
				"", //
				PointType.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCEvt.values())), //	
		


		
		
		MODULE_3_I_D(new PointImpl(//
				"S160_MODULE_3_I_D", //
				"Module3Id", //
				"Module3Id", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_3_I_D_STR(new PointImpl(//
				"S160_MODULE_3_I_D_STR", //
				"Module3IdStr", //
				"Module3IdStr", //
				"", //
				PointType.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_3_D_C_A(new PointImpl(//
				"S160_MODULE_3_D_C_A", //
				"Module3Dca", //
				"Module3Dca", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF", //
				new OptionsEnum[0])), //	
		MODULE_3_D_C_V(new PointImpl(//
				"S160_MODULE_3_D_C_V", //
				"Module3Dcv", //
				"Module3Dcv", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF", //
				new OptionsEnum[0])), //	
		MODULE_3_D_C_W(new PointImpl(//
				"S160_MODULE_3_D_C_W", //
				"Module3Dcw", //
				"Module3Dcw", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF", //
				new OptionsEnum[0])), //	
		MODULE_3_D_C_W_H(new PointImpl(//
				"S160_MODULE_3_D_C_W_H", //
				"Module3Dcwh", //
				"Module3Dcwh", //
				"", //
				PointType.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF", //
				new OptionsEnum[0])), //	
		MODULE_3_TMS(new PointImpl(//
				"S160_MODULE_3_TMS", //
				"Module3Tms", //
				"Module3Tms", //
				"", //
				PointType.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_3_TMP(new PointImpl(//
				"S160_MODULE_3_TEMP", //
				"Module3Temp", //
				"Module3Temp", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_3_D_C_ST(new PointImpl(//
				"S160_MODULE_3_OP_STATES", //
				"Module3OpStates", //
				"Module3OpStates", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCSt.values())), //	
		MODULE_3_EVT(new PointImpl(//
				"S160_MODULE_3_EVENTS", //
				"Module3Events", //
				"Module3Events", //
				"", //
				PointType.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCEvt.values())), //	
		

		
		
		
		
		MODULE_4_I_D(new PointImpl(//
				"S160_MODULE_4_I_D", //
				"Module4Id", //
				"Module4Id", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_4_I_D_STR(new PointImpl(//
				"S160_MODULE_4_I_D_STR", //
				"Module4IdStr", //
				"Module4IdStr", //
				"", //
				PointType.STRING8, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_4_D_C_A(new PointImpl(//
				"S160_MODULE_4_D_C_A", //
				"Module4Dca", //
				"Module4Dca", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.AMPERE, //
				"DCA_SF", //
				new OptionsEnum[0])), //	
		MODULE_4_D_C_V(new PointImpl(//
				"S160_MODULE_4_D_C_V", //
				"Module4Dcv", //
				"Module4Dcv", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.VOLT, //
				"DCV_SF", //
				new OptionsEnum[0])), //	
		MODULE_4_D_C_W(new PointImpl(//
				"S160_MODULE_4_D_C_W", //
				"Module4Dcw", //
				"Module4Dcw", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT, //
				"DCW_SF", //
				new OptionsEnum[0])), //	
		MODULE_4_D_C_W_H(new PointImpl(//
				"S160_MODULE_4_D_C_W_H", //
				"Module4Dcwh", //
				"Module4Dcwh", //
				"", //
				PointType.ACC32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.WATT_HOURS, //
				"DCWH_SF", //
				new OptionsEnum[0])), //	
		MODULE_4_TMS(new PointImpl(//
				"S160_MODULE_4_TMS", //
				"Module4Tms", //
				"Module4Tms", //
				"", //
				PointType.UINT32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_4_TMP(new PointImpl(//
				"S160_MODULE_4_TEMP", //
				"Module4Temp", //
				"Module4Temp", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.DEGREE_CELSIUS, //
				null, //
				new OptionsEnum[0])), //	
		MODULE_4_D_C_ST(new PointImpl(//
				"S160_MODULE_4_OP_STATES", //
				"Module4OpStates", //
				"Module4OpStates", //
				"", //
				PointType.ENUM16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCSt.values())), //	
		MODULE_4_EVT(new PointImpl(//
				"S160_MODULE_4_EVENTS", //
				"Module4Events", //
				"Module4Events", //
				"", //
				PointType.BITFIELD32, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				S160_DCEvt.values())) //	
		; //

		protected final PointImpl impl;

		private S160(PointImpl impl) {
			this.impl = impl;
		}

		@Override
		public PointImpl get() {
			return this.impl;
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
	
	public static enum S160_DCEvt implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		GROUND_FAULT(0, "Ground Fault"), //
		INPUT_OVER_VOLTAGE(1, "Input over voltage"), //
		RESERVED_2(2, "Reserved 2"), //
		DC_DISCONNECT(3, "DC Disconnect"), //
		RESERVED_4(4, "Reserved 4"), //
		CABINET_OPEN(5, "Cabinet open"), //
		MANUAL_SHUTDOWN(6, "Manual Shutdown"), //
		OVER_TEMP(7, "Over Temp"), //
		RESERVED_8(8, "Reserved 8"), //
		RESERVED_9(9, "Reserved 9"), //
		RESERVED_10(10, "Reserved 10"), //
		RESERVED_11(11, "Reserved 11"), //
		BLOWN_FUSE(12, "Blown Fuse"), //
		UNDER_TEMP(13, "Unbder Temp"), //
		MEMORY_LOSS(14, "Memory Loss"), //
		ARC_DETECTION(15, "Arc Detection"), //
		RESERVED_16(16, "Reserved 16"), //
		RESERVED_17(17, "Reserved 17"), //
		RESERVED_18(18, "Reserved 18"), //
		RESERVED_19(19, "Reserved 19"), //
		TEST_FAILED(20, "Test failed"), //
		INPUT_UNDER_VOLTAGE(21, "Input under voltage"), //
		INPUT_OVER_CURRENT(22, "Inout over current"); //

		
		private final int value;
		private final String name;
		
		private S160_DCEvt(int value, String name) {
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
