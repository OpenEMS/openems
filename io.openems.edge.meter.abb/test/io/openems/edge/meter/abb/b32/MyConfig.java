package io.openems.edge.meter.abb.b32;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String mbusId;
		private MeterType type;
		private int primaryAddress;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMbusId(String mbusId) {
			this.mbusId = mbusId;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setPrimaryAddress(int primaryAddress) {
			this.primaryAddress = primaryAddress;
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
	public int primaryAddress() {
		return this.builder.primaryAddress;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String mbus_id() {
		return this.builder.mbusId;
	}
}