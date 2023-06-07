package io.openems.edge.evcs.ocpp.ies.keywatt.singleccs;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int connectorId;
		private String ocppId;
		private boolean debugMode;
		private int minHwPower;
		private int maxHwPower;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setConnectorId(int connectorId) {
			this.connectorId = connectorId;
			return this;
		}

		public Builder setOcppId(String ocppId) {
			this.ocppId = ocppId;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setMinHwPower(int minHwPower) {
			this.minHwPower = minHwPower;
			return this;
		}

		public Builder setMaxHwPower(int maxHwPower) {
			this.maxHwPower = maxHwPower;
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
	public String ocpp_id() {
		return this.builder.ocppId;
	}

	@Override
	public int connectorId() {
		return this.builder.connectorId;
	}

	@Override
	public int maxHwPower() {
		return this.builder.maxHwPower;
	}

	@Override
	public int minHwPower() {
		return this.builder.minHwPower;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}
}