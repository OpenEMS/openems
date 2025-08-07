package io.openems.edge.evse.chargepoint.keba.common;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

import io.openems.common.types.OptionsEnum;

public record ProductTypeAndFeatures(ProductFamily productFamily, DeviceCurrent deviceCurrent, Connector connector,
		Phases phases, Metering metering, Rfid rfid, Button button) {

	/**
	 * Parses a Long value to a {@link ProductTypeAndFeatures} record.
	 * 
	 * @param value the value
	 * @return the record
	 */
	public static ProductTypeAndFeatures from(Long value) {
		final ProductFamily productFamily;
		final DeviceCurrent deviceCurrent;
		final Connector connector;
		final Phases phases;
		final Metering metering;
		final Rfid rfid;
		final Button button;

		if (value == null) {
			button = Button.UNDEFINED;
			rfid = Rfid.UNDEFINED;
			metering = Metering.UNDEFINED;
			phases = Phases.UNDEFINED;
			connector = Connector.UNDEFINED;
			deviceCurrent = DeviceCurrent.UNDEFINED;
			productFamily = ProductFamily.UNDEFINED;

		} else {
			final var val = new AtomicLong(value * 10);
			IntSupplier nextDigit = () -> (int) val.updateAndGet(v -> v / 10) % 10;

			button = OptionsEnum.<Button>getOptionOrUndefined(Button.class, nextDigit.getAsInt());
			rfid = OptionsEnum.<Rfid>getOptionOrUndefined(Rfid.class, nextDigit.getAsInt());
			metering = OptionsEnum.<Metering>getOptionOrUndefined(Metering.class, nextDigit.getAsInt());
			phases = OptionsEnum.<Phases>getOptionOrUndefined(Phases.class, (int) nextDigit.getAsInt());
			connector = OptionsEnum.<Connector>getOptionOrUndefined(Connector.class, nextDigit.getAsInt());
			deviceCurrent = OptionsEnum.<DeviceCurrent>getOptionOrUndefined(DeviceCurrent.class, nextDigit.getAsInt());
			productFamily = OptionsEnum.<ProductFamily>getOptionOrUndefined(ProductFamily.class, nextDigit.getAsInt());
		}

		return new ProductTypeAndFeatures(productFamily, deviceCurrent, connector, phases, metering, rfid, button);
	}

	public enum ProductFamily implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		KC_P30(3, "KC-P30"), //
		KC_P40(4, "KC-P40");

		private final int value;
		private final String name;

		private ProductFamily(int value, String name) {
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

	public enum DeviceCurrent implements OptionsEnum {
		UNDEFINED(-1, "Undefined", -1), //
		A16_16(1, "16...16 A", 16000), //
		A32_32(2, "32...32 A", 32000);

		public final int milliamps;

		private final int value;
		private final String name;

		private DeviceCurrent(int value, String name, int milliamps) {
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

	public enum Connector implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		CABLE(1, "Cable"), // ("C", "P", "T", "N" in the product key under connector)
		SOCKET(2, "Socket"); // ("S", "R" in the product key under connector)

		private final int value;
		private final String name;

		private Connector(int value, String name) {
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

	public enum Phases implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		SINGLE_PHASE(1, "1...1-phase"), //
		THREE_PHASE(2, "3...3-phase"), //
		PHASE_SWITCHING(3, "S...3to1 switching"), //
		ROTATION(4, "Rotation");

		private final int value;
		private final String name;

		private Phases(int value, String name) {
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

	public enum Metering implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NONE(0, "None"), // ("0" in product key)
		ENERGY_METER(1, "E...Energy meter"), //
		MID_METER(2, "M...MID meter"), // MID (Measuring Instruments Directive)
		LEGAL(3, "L...Legal Meter (MessEV)");

		private final int value;
		private final String name;

		private Metering(int value, String name) {
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

	public enum Rfid implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NO_RFID(0, "No RFID"), //
		WITH_RFID(1, "With RFID");

		private final int value;
		private final String name;

		private Rfid(int value, String name) {
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

	public enum Button implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NO_BUTTON(0, "No Button"), //
		WITH_BUTTON(1, "With Button");

		private final int value;
		private final String name;

		private Button(int value, String name) {
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