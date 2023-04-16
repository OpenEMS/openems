package io.openems.edge.meter.virtual.add;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.virtual.add.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String id = null;
		public MeterType type;
		public boolean addToSum;
		public String[] meterIds;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setAddToSum(boolean addToSum) {
			this.addToSum = addToSum;
			return this;
		}

		public Builder setMeterIds(String... meterIds) {
			this.meterIds = meterIds;
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
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public boolean addToSum() {
		return this.builder.addToSum;
	}

	@Override
	public String[] meterIds() {
		return this.builder.meterIds;
	}

}
