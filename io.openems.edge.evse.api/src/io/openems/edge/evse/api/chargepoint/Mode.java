package io.openems.edge.evse.api.chargepoint;

import io.openems.common.types.OptionsEnum;

public enum Mode {
	SMART, ZERO, MINIMUM, FORCE;

	public enum Priority {
		CAR, STORAGE;
	}

	// public static sealed interface Profile {
	// public static record PhaseSwitch(Phase currentPhase, Map<Phase, ChargeParams>
	// chargeParams) implements Profile {
	// }
	// }

	public enum Actual implements OptionsEnum {
		ZERO(0, "Zero"), //
		MINIMUM(1, "Minimum charge"), // Avoid interrupting of old EVs
		FORCE(3, "Force charge") //
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
			return ZERO;
		}
	}
}
