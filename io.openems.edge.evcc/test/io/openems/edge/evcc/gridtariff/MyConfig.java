package io.openems.edge.evcc.gridtariff;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcc.gridtariff.Config;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String apiUrl;
		private LogVerbosity logVerbosity;

		private Builder() {
			// empty
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
			return this;
		}

		public Builder setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
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
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}

	@Override
	public String apiUrl() {
		return this.builder.apiUrl;
	}

}