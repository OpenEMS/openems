package io.openems.edge.timeofusetariff.energypriceforecast;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {
	public static class Builder {
		private String id = "tariffForecast0";
		private boolean enabled = true;
		private int maxForecastAgeHours = 24;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setMaxForecastAgeHours(int hours) {
			this.maxForecastAgeHours = hours;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates a configuration builder.
	 *
	 * @return the builder
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
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public int service_ranking() {
		return -100;
	}

	@Override
	public int refreshIntervalMinutes() {
		return 30;
	}

	@Override
	public int maxForecastAgeHours() {
		return this.builder.maxForecastAgeHours;
	}
}
