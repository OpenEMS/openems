package io.openems.edge.controller.ess.activepowervoltagecharacteristic;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private int nominalVoltage;
		private int waitForHysteresis;
		private String powerVoltConfig;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setNominalVoltage(int nominalVoltage) {
			this.nominalVoltage = nominalVoltage;
			return this;
		}

		public Builder setPowerVoltConfig(String powerVoltConfig) throws OpenemsNamedException {
			this.powerVoltConfig = powerVoltConfig;
			return this;
		}

		public Builder setWaitForHysteresis(int waitForHysteresis) throws OpenemsNamedException {
			this.waitForHysteresis = waitForHysteresis;
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public String lineConfig() {
		return this.builder.powerVoltConfig;
	}

	@Override
	public float nominalVoltage() {
		return this.builder.nominalVoltage;
	}

	@Override
	public int waitForHysteresis() {
		return this.builder.waitForHysteresis;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

	@Override
	public String meter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.meter_id());
	}
}