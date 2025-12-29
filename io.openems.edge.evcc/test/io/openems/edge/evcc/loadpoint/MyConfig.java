package io.openems.edge.evcc.loadpoint;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias = "";
		private boolean enabled = true;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private String apiUrl = "http://localhost:7070/api/state";
		private String loadpointTitle = "";
		private int loadpointIndex = 0;
		private int minChargingPowerW = 1380;
		private int maxChargingPowerW = 22080;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
			return this;
		}

		public Builder setLoadpointTitle(String loadpointTitle) {
			this.loadpointTitle = loadpointTitle;
			return this;
		}

		public Builder setLoadpointIndex(int loadpointIndex) {
			this.loadpointIndex = loadpointIndex;
			return this;
		}

		public Builder setMinChargingPowerW(int minChargingPowerW) {
			this.minChargingPowerW = minChargingPowerW;
			return this;
		}

		public Builder setMaxChargingPowerW(int maxChargingPowerW) {
			this.maxChargingPowerW = maxChargingPowerW;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates a new configuration builder.
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
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String apiUrl() {
		return this.builder.apiUrl;
	}

	@Override
	public String loadpointTitle() {
		return this.builder.loadpointTitle;
	}

	@Override
	public int loadpointIndex() {
		return this.builder.loadpointIndex;
	}

	@Override
	public int minChargingPowerW() {
		return this.builder.minChargingPowerW;
	}

	@Override
	public int maxChargingPowerW() {
		return this.builder.maxChargingPowerW;
	}
}
