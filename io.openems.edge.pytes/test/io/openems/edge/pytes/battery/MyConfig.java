package io.openems.edge.pytes.battery;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId = "ess0";
		private int maxChargeCurrent = 40;
		private int maxDischargeCurrent = 40;
		private int capacity = 0;
		private boolean debugMode = false;

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

		public Builder setMaxChargeCurrent(int maxChargeCurrent) {
			this.maxChargeCurrent = maxChargeCurrent;
			return this;
		}

		public Builder setMaxDischargeCurrent(int maxDischargeCurrent) {
			this.maxDischargeCurrent = maxDischargeCurrent;
			return this;
		}

		public Builder setCapacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
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
	public int maxChargeCurrent() {
		return this.builder.maxChargeCurrent;
	}

	@Override
	public int maxDischargeCurrent() {
		return this.builder.maxDischargeCurrent;
	}

	@Override
	public int capacity() {
		return this.builder.capacity;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.builder.essId);
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), "modbus0");
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

}
