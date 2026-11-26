package io.openems.edge.controller.ortsnetzauslastung;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.DebugMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String url;
		private String meterId;
		private DebugMode debugMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setDebugMode(DebugMode debugMode) {
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
	public String url() {
		return this.builder.url;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public DebugMode debugMode() {
		return this.builder.debugMode;
	}
}
