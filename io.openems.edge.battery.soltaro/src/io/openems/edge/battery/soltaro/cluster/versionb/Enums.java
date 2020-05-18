package io.openems.edge.battery.soltaro.cluster.versionb;

import io.openems.common.types.OptionsEnum;

public class Enums {

	public enum StartStop implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		START(1, "Start"), //
		STOP(2, "Stop");

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
		UNDEFINED(-1, "Undefined"), //
		USED(1, "Rack is used"), //
		UNUSED(2, "Rack is not used");

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
		UNDEFINED(-1, "Undefined"), //
		STANDING(0, "Standby"), //
		DISCHARGING(1, "Discharging"), //
		CHARGING(2, "Charging");

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
		UNDEFINED(-1, "Undefined"), //
		NORMAL(0, "Normal"), // Allow discharge and charge
		FULLY_CHARGED(1, "Fully charged"), // Allow discharge, deny charge
		EMPTY(2, "Empty"), // Allow charge, deny discharge
		STANDBY(3, "Standby"), // deny discharge, deny charge
		STOPPED(4, "Stopped"); // deny discharge, deny charge

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

	public enum ClusterRunState implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NORMAL(0, "Normal"), //
		STOP_CHARGING(1, "Stop charging"), //
		STOP_DISCHARGE(2, "Stop discharging"), //
		STANDBY(3, "Standby");

		private int value;
		private String name;

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

	public enum ContactorControl implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		CUT_OFF(0, "Cut off"), //
		CONNECTION_INITIATING(1, "Connection initiating"), //
		ON_GRID(3, "On grid");

		int value;
		String name;

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
}
