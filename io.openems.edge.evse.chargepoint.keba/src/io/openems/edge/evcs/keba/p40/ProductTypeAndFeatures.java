package io.openems.edge.evcs.keba.p40;

import io.openems.common.types.OptionsEnum;

public record ProductTypeAndFeatures(ProductType productType, CableOrSocket cableOrSocket,
		SupportedCurrent supportedCurrent, DeviceSeries deviceSeries, EnergyMeter energyMeter,
		Authorization authorization) {

	/**
	 * Parses a Long value to a {@link ProductTypeAndFeatures} record.
	 * 
	 * @param value the value
	 * @return the record
	 */
	public static ProductTypeAndFeatures from(Long value) {
		if (value == null) {
			return new ProductTypeAndFeatures(ProductType.UNDEFINED, CableOrSocket.UNDEFINED,
					SupportedCurrent.UNDEFINED, DeviceSeries.UNDEFINED, EnergyMeter.UNDEFINED, Authorization.UNDEFINED);
		}

		return new ProductTypeAndFeatures(
				OptionsEnum.<ProductType>getOptionOrUndefined(ProductType.class, (int) ((value /= 10) % 10)), //
				OptionsEnum.<CableOrSocket>getOptionOrUndefined(CableOrSocket.class, (int) ((value /= 10) % 10)), //
				OptionsEnum.<SupportedCurrent>getOptionOrUndefined(SupportedCurrent.class, (int) ((value /= 10) % 10)), //
				OptionsEnum.<DeviceSeries>getOptionOrUndefined(DeviceSeries.class, (int) ((value /= 10) % 10)), //
				OptionsEnum.<EnergyMeter>getOptionOrUndefined(EnergyMeter.class, (int) ((value /= 10) % 10)), //
				OptionsEnum.<Authorization>getOptionOrUndefined(Authorization.class, (int) (value % 10)));
	}

	public enum ProductType implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		KC_P30(3, "KC-P30");

		private final int value;
		private final String name;

		private ProductType(int value, String name) {
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

	public enum CableOrSocket implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		SOCKET(0, "Socket"), //
		CABLE(1, "Cable");

		private final int value;
		private final String name;

		private CableOrSocket(int value, String name) {
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

	public enum SupportedCurrent implements OptionsEnum {
		UNDEFINED(-1, "Undefined", -1), //
		A13(1, "13 A", 13000), //
		A16(2, "16 A", 16000), //
		A20(3, "20 A", 20000), //
		A32(4, "32 A", 32000);

		public final int milliamps;

		private final int value;
		private final String name;

		private SupportedCurrent(int value, String name, int milliamps) {
			this.value = value;
			this.name = name;
			this.milliamps = milliamps;
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

	public enum DeviceSeries implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		X_SERIES(0, "x-series"), //
		C_SERIES(1, "c-series");

		private final int value;
		private final String name;

		private DeviceSeries(int value, String name) {
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

	public enum EnergyMeter implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		STANDARD(1, "Standard energy meter, not calibrated"), //
		MID(2, "Calibratable energy meter, MID"), //
		CERTIFIED(3, "Calibratable measuring instrument for electrical energy with national certification");

		private final int value;
		private final String name;

		private EnergyMeter(int value, String name) {
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

	public enum Authorization implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NO_RFID(0, "No RFID"), //
		WITH_RFID(1, "With RFID");

		private final int value;
		private final String name;

		private Authorization(int value, String name) {
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
}