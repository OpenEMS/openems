package io.openems.edge.controller.api.rest.readwrite;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = "ctrlApiRest0";
		public int port;
		public int connectionlimit;
		public int apiTimeout;
		public boolean debugMode;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
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

		public Builder setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
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
	public int port() {
		return this.builder.port;
	}

	@Override
	public int connectionlimit() {
		return this.builder.connectionlimit;
	}

	@Override
	public int apiTimeout() {
		return this.builder.apiTimeout;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

}