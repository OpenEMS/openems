package io.openems.edge.pvinverter.cluster;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String[] pvInverterIds;
		private MeterType meterType = MeterType.PRODUCTION;
		private boolean addToSum = true;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPvInverterIds(String... pvInverterIds) {
			this.pvInverterIds = pvInverterIds;
			return this;
		}

		public Builder setMeterType(MeterType meterType) {
			this.meterType = meterType;
			return this;
		}

		public Builder setAddToSum(boolean addToSum) {
			this.addToSum = addToSum;
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
	public String[] pvInverter_ids() {
		return this.builder.pvInverterIds;
	}

	@Override
	public MeterType meterType() {
		return this.builder.meterType;
	}

	@Override
	public boolean addToSum() {
		return this.builder.addToSum;
	}
}