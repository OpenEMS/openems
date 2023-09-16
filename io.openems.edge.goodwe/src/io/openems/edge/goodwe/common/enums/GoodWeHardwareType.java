package io.openems.edge.goodwe.common.enums;

import java.util.stream.Stream;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.OptionsEnum;

/**
 * Defines the GoodWe hardware type.
 * 
 * <p>
 * The hardware type provides the information about the maximum DC current and a
 * filter that identifies the hardware type by the serial number, as the
 * Register for {@link GoodWeType} is not supported for GoodWe 15, 20 & 29.9.
 */
public enum GoodWeHardwareType implements OptionsEnum {
	UNDEFINED(-1, "Undefined", (t) -> false, 0), //
	OTHER(0, "Other", (t) -> false, 25), //
	GOODWE_10(1, "GoodWe 10kW", Position2Filter.of("10"), 25), //
	GOODWE_20(2, "GoodWe 20kW", Position2Filter.of("20"), 50), //
	GOODWE_29_9(3, "GoodWe 29,9kW", Home30Filter.of("29K9", "30"), 50); //

	public final int value;
	public final String type;
	public final ThrowingFunction<String, Boolean, Exception> serialNrFilter;
	public final int maxDcCurrent; // [A]

	private static class Position2Filter implements ThrowingFunction<String, Boolean, Exception> {
		private final String match;

		public static Position2Filter of(String match) {
			return new Position2Filter(match);
		}

		private Position2Filter(String match) {
			this.match = match;
		}

		@Override
		public Boolean apply(String serialNr) throws Exception {
			return this.match.equals(serialNr.substring(2, 4));
		}
	}

	private static class Home30Filter implements ThrowingFunction<String, Boolean, Exception> {
		private final String[] match;

		public static Home30Filter of(String... match) {
			return new Home30Filter(match);
		}

		private Home30Filter(String... match) {
			this.match = match;
		}

		@Override
		public Boolean apply(String serialNr) throws Exception {
			return Stream.of(this.match) //
					.filter(t -> t.equals(serialNr.substring(2, 4)) || serialNr.substring(1, 5).contains(t)).findFirst() //
					.isPresent();
		}
	}

	private GoodWeHardwareType(int value, String type, ThrowingFunction<String, Boolean, Exception> serialNrFilter,
			int maxDcCurrent) {
		this.value = value;
		this.type = type;
		this.serialNrFilter = serialNrFilter;
		this.maxDcCurrent = maxDcCurrent;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.type;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}