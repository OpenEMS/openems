package io.openems.edge.simulator.battery;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int minCellVoltage;
		private int voltage;
		private int capacityKWh;
		private int temperature;
		private int soh;
		private int soc;
		private int chargeMaxCurrent;
		private int disChargeMaxCurrent;
		private int chargeMaxVoltage;
		private int disChargeMinVoltage;
		private int numberOfSlaves;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMinCellVoltage_mV(int minCellVoltage) {
			this.minCellVoltage = minCellVoltage;
			return this;
		}

		public Builder setVoltage(int voltage) {
			this.voltage = voltage;
			return this;
		}

		public Builder setCapacityKWh(int capacityKWh) {
			this.capacityKWh = capacityKWh;
			return this;
		}

		public Builder setTemperature(int temperature) {
			this.temperature = temperature;
			return this;
		}

		public Builder setSoh(int soh) {
			this.soh = soh;
			return this;
		}

		public Builder setSoc(int soc) {
			this.soc = soc;
			return this;
		}

		public Builder setChargeMaxCurrent(int chargeMaxCurrent) {
			this.chargeMaxCurrent = chargeMaxCurrent;
			return this;
		}

		public Builder setDisChargeMaxCurrent(int disChargeMaxCurrent) {
			this.disChargeMaxCurrent = disChargeMaxCurrent;
			return this;
		}

		public Builder setChargeMaxVoltage(int chargeMaxVoltage) {
			this.chargeMaxVoltage = chargeMaxVoltage;
			return this;
		}

		public Builder setDisChargeMinVoltage(int disChargeMinVoltage) {
			this.disChargeMinVoltage = disChargeMinVoltage;
			return this;
		}

		public Builder setNumberOfSlaves(int numberOfSlaves) {
			this.numberOfSlaves = numberOfSlaves;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public int numberOfSlaves() {
		return this.builder.numberOfSlaves;
	}

	@Override
	public int disChargeMinVoltage() {
		return this.builder.disChargeMinVoltage;
	}

	@Override
	public int chargeMaxVoltage() {
		return this.builder.chargeMaxVoltage;
	}

	@Override
	public int disChargeMaxCurrent() {
		return this.builder.disChargeMaxCurrent;
	}

	@Override
	public int chargeMaxCurrent() {
		return this.builder.chargeMaxCurrent;
	}

	@Override
	public int soc() {
		return this.builder.soc;
	}

	@Override
	public int soh() {
		return this.builder.soh;
	}

	@Override
	public int temperature() {
		return this.builder.temperature;
	}

	@Override
	public int capacityKWh() {
		return this.builder.capacityKWh;
	}

	@Override
	public int voltage() {
		return this.builder.voltage;
	}

	@Override
	public int minCellVoltage_mV() {
		return this.builder.minCellVoltage;
	}
}