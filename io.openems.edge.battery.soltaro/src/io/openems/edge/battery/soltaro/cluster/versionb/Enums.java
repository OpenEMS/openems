package io.openems.edge.battery.soltaro.cluster.versionb;

import io.openems.common.types.OptionsEnum;

public class Enums {

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

	public enum ChargeIndication implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		STANDBY(0, "Standby"), //
		DISCHARGE(1, "Dischare"), //
		CHARGE(2, "Charge");

		private int value;
		private String name;

		private ChargeIndication(int value, String name) {
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
