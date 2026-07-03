package io.openems.edge.braiinsos;

import io.openems.common.types.OptionsEnum;

public enum Mode {
	READ_ONLY(null), //
	MANUAL_ON(Actual.ON), //
	MANUAL_OFF(Actual.OFF);

	public final Mode.Actual actual;

	private Mode(Mode.Actual actual) {
		this.actual = actual;
	}

	public enum Actual implements OptionsEnum {
		OFF(0, "Off"), //
		ON(1, "On"), //
		;

		private final int value;
		private final String name;

		private Actual(int value, String name) {
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
			return OFF;
		}
	}
}
