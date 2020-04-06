package io.openems.edge.battery.soltaro.single.versionc;

import io.openems.common.types.OptionsEnum;

public class Enums {

	public enum FanStatus implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		OPEN(0x1, "Open"), //
		CLOSE(0x2, "Close");

		private final int value;
		private final String name;

		private FanStatus(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum ContactorState implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		START(0x1, "Start"), //
		STOP(0x2, "Stop");

		private final int value;
		private final String name;

		private ContactorState(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum ContactExport implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		HIGH(0x1, "High"), //
		LOW(0x2, "Low");

		private final int value;
		private final String name;

		private ContactExport(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum SystemRunMode implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NORMAL(0x1, "Normal"), //
		DEBUG(0x2, "Debug");

		private final int value;
		private final String name;

		private SystemRunMode(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum PreContactorState implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		START(0x1, "Start"), //
		STOP(0x2, "Stop");

		private final int value;
		private final String name;

		private PreContactorState(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum ShortCircuitFunction implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		ENABLE(0x1, "Enable"), //
		DISABLE(0x2, "Disable");

		private final int value;
		private final String name;

		private ShortCircuitFunction(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum AutoSetFunction implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		INIT_MODE(0x0, "Init mode"), //
		START_AUTO_SETTING(0x1, "Start auto setting"), //
		SUCCES(0x2, "Success"), //
		FAILURE(0x3, "Failure");

		private final int value;
		private final String name;

		private AutoSetFunction(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum ContactorControl implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		CUT_OFF(0, "Cut off"), //
		CONNECTION_INITIATING(1, "Connection initiating"), //
		ON_GRID(3, "On grid");

		private final int value;
		private final String name;

		private ContactorControl(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	public enum ClusterRunState implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NORMAL(0, "Normal"), //
		FULL(1, "Full"), //
		EMPTY(2, "Empty"), //
		STANDBY(3, "Standby"), //
		STOP(4, "Stop");

		private final int value;
		private final String name;

		private ClusterRunState(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}
}