package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public class SafetyParameterEnums {

	public class Vrt {

		public enum CurrentDistributionMode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			REACTIVE_POWER_PRIO(0, "Reactive Power Priority Mode"), //
			ACTIVE_POWER_PRIO(1, "Active Power Priority Mode"), //
			CONSTANT_CURRENT(2, "Constant Current Mode");

			private final int value;
			private final String option;

			private CurrentDistributionMode(int value, String option) {
				this.value = value;
				this.option = option;
			}

			@Override
			public int getValue() {
				return this.value;
			}

			@Override
			public String getName() {
				return this.option;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}

		public enum GeneralRecoveryMode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			DISABLE(0, "Disable"), //
			GRADIENT_CONTROL(1, "Gradient Control"), //
			PT_1_BEHAVIOUR(2, "PT-1 Behaviour");

			private final int value;
			private final String option;

			private GeneralRecoveryMode(int value, String option) {
				this.value = value;
				this.option = option;
			}

			@Override
			public int getValue() {
				return this.value;
			}

			@Override
			public String getName() {
				return this.option;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}
		}
	}

	public class Rpm {

		public enum Mode implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			BASIC(0, "Basic"), //
			SLOPE(1, "Slope Model");

			private final int value;
			private final String option;

			private Mode(int value, String option) {
				this.value = value;
				this.option = option;
			}

			@Override
			public int getValue() {
				return this.value;
			}

			@Override
			public String getName() {
				return this.option;
			}

			@Override
			public OptionsEnum getUndefined() {
				return UNDEFINED;
			}

		}
	}
}
