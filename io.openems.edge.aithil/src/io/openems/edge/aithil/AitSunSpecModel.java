package io.openems.edge.aithil;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;

public enum AitSunSpecModel implements SunSpecModel {
	S_134(//
			"", //
			"", //
			"", //
			52, //
			AitSunSpecModel.S134.values(), //
			SunSpecModelType.VENDOR_SPECIFIC //
	); //

	public static enum S134 implements SunSpecPoint {
		ACT_CRV(new PointImpl(//
				"S134_ACT_CRV", //
				"ActCrv", //
				"Index of active curve. 0=no active curve", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		// TODO Enum Enabled: 0
		MOD_ENA(new PointImpl(//
				"S134_MOD_ENA", //
				"ModEna", //
				"Is curve-based Frequency-Watt control active.", //
				"", //
				PointType.BITFIELD16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		WIN_TMS(new PointImpl(//
				"S134_WIN_TMS", //
				"WinTms", //
				"Time window for freq-watt change", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		RVRT_TMS(new PointImpl(//
				"S134_RVRT_TMS", //
				"RvrtTms", //
				"Timeout period for freq-watt curve selection.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		RMP_TMS(new PointImpl(//
				"S134_RMP_TMS", //
				"RmpTms", //
				"Ramp time for moving from current mode to new mode.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.SECONDS, //
				null, //
				new OptionsEnum[0])), //
		N_CRV(new PointImpl(//
				"S134_N_CRV", //
				"NCrv", //
				"Number of curves supported (recommend min. 4).", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		N_PT(new PointImpl(//
				"S134_N_PT", //
				"NPt", //
				"Number of curve points supported (maximum of 10).", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		HZ_SF(new PointImpl(//
				"S134_HZ_SF", //
				"Hz_SF", //
				"Scale factor for frequency.", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		W_SF(new PointImpl(//
				"S134_W_SF", //
				"W_SF", //
				"Scale factor for percent WRef.", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		RMP_INC_DEC_SF(new PointImpl(//
				"S134_RMP_INC_DEC_SF", //
				"RmpIncDec_SF", //
				"Scale factor for increment and decrement ramps.", //
				"", //
				PointType.SUNSSF, //
				false, //
				AccessMode.READ_ONLY, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //

		// Repeating
		ACT_PT(new PointImpl(//
				"S134_ACT_PV", //
				"ActPt", //
				"Number of active points in array.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.NONE, //
				null, //
				new OptionsEnum[0])), //
		HZ1(new PointImpl(//
				"S134_HZ1", //
				"Hz1", //
				"Point 1 Hertz.", //
				"", //
				PointType.UINT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W1(new PointImpl(//
				"S134_W1", //
				"W1", //
				"Point 1 Watts.", //
				"", //
				PointType.INT16, //
				true, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ2(new PointImpl(//
				"S134_HZ2", //
				"Hz2", //
				"Point 2 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W2(new PointImpl(//
				"S134_W2", //
				"W2", //
				"Point 2 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ3(new PointImpl(//
				"S134_HZ3", //
				"Hz3", //
				"Point 3 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W3(new PointImpl(//
				"S134_W3", //
				"W3", //
				"Point 3 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ4(new PointImpl(//
				"S134_HZ4", //
				"Hz4", //
				"Point 4 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W4(new PointImpl(//
				"S134_W4", //
				"W4", //
				"Point 4 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ5(new PointImpl(//
				"S134_HZ5", //
				"Hz5", //
				"Point 5 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W5(new PointImpl(//
				"S134_W5", //
				"W5", //
				"Point 5 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ6(new PointImpl(//
				"S134_HZ6", //
				"Hz5", //
				"Point 6 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W6(new PointImpl(//
				"S134_W6", //
				"W6", //
				"Point 6 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ7(new PointImpl(//
				"S134_HZ7", //
				"Hz7", //
				"Point 7 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W7(new PointImpl(//
				"S134_W7", //
				"W7", //
				"Point 7 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ8(new PointImpl(//
				"S134_HZ8", //
				"Hz8", //
				"Point 8 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W8(new PointImpl(//
				"S134_W8", //
				"W8", //
				"Point 8 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		HZ9(new PointImpl(//
				"S134_HZ9", //
				"Hz9", //
				"Point 9 Hertz.", //
				"", //
				PointType.UINT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.HERTZ, //
				"HZ_SF", //
				new OptionsEnum[0])), //
		W9(new PointImpl(//
				"S134_W9", //
				"W9", //
				"Point 9 Watts.", //
				"", //
				PointType.INT16, //
				false, //
				AccessMode.READ_WRITE, //
				Unit.PERCENT, //
				"W_SF", //
				new OptionsEnum[0])), //
		;

		protected final PointImpl impl;

		private S134(PointImpl impl) {
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

	private AitSunSpecModel(String label, String description, String notes, int length, SunSpecPoint[] points,
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
