package io.openems.edge.controller.api.rest.readonly;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		public boolean enabled;
		private int port;
		private int connectionlimit;
		private boolean debugMode;

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

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setConnectionlimit(int connectionlimit) {
			this.connectionlimit = connectionlimit;
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

	protected static Builder create() {
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
	public int port() {
		return this.builder.port;
	}

	@Override
	public int connectionlimit() {
		return this.builder.connectionlimit;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

}