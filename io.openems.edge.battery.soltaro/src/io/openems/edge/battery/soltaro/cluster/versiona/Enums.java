package io.openems.edge.battery.soltaro.cluster.versiona;

import io.openems.common.types.OptionsEnum;

public class Enums {

	public enum StartStop implements OptionsEnum {

		UNDEFINED(-1, "Undefined"), START(1, "Start"), STOP(2, "Stop");

		int value;
		String name;

		private StartStop(int value, String name) {
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

	public enum RackUsage implements OptionsEnum {

		UNDEFINED(-1, "Undefined"), USED(1, "Rack is used"), UNUSED(2, "Rack is not used");

		int value;
		String name;

		private RackUsage(int value, String name) {
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

	public enum ChargeIndication implements OptionsEnum {

		UNDEFINED(-1, "Undefined"), STANDING(0, "Standby"), DISCHARGING(1, "Discharging"), CHARGING(2, "Charging");

		private int value;
		private String name;

		private ChargeIndication(int value, String name) {
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

	public enum RunningState implements OptionsEnum {

		UNDEFINED(-1, "Undefined"), NORMAL(0, "Normal"), FULLY_CHARGED(1, "Fully charged"), EMPTY(2, "Empty"),
		STANDBY(3, "Standby"), STOPPED(4, "Stopped");

		int value;
		String name;

		private RunningState(int value, String name) {
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
