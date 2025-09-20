package io.openems.edge.evcs.ocpp.abl;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int minHwCurrent;
		private int maxHwCurrent;
		private String limitId;
		private String logicalId;
		private int connectorId;
		private String ocppId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMinHwCurrent(int minHwCurrent) {
			this.minHwCurrent = minHwCurrent;
			return this;
		}

		public Builder setMaxHwCurrent(int maxHwCurrent) {
			this.maxHwCurrent = maxHwCurrent;
			return this;
		}

		public Builder setLimitId(String limitId) {
			this.limitId = limitId;
			return this;
		}

		public Builder setLogicalId(String logicalId) {
			this.logicalId = logicalId;
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
	public String logicalId() {
		return this.builder.logicalId;
	}

	@Override
	public String limitId() {
		return this.builder.limitId;
	}

	@Override
	public int maxHwCurrent() {
		return this.builder.maxHwCurrent;
	}

	@Override
	public int minHwCurrent() {
		return this.builder.minHwCurrent;
	}
}