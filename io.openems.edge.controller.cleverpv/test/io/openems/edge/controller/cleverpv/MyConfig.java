package io.openems.edge.controller.cleverpv;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.DebugMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private String url;
		private ControlMode controlMode;
		private DebugMode debugMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder setMode(ControlMode controlMode) {
			this.controlMode = controlMode;
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
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}

	@Override
	public DebugMode debugMode() {
		return this.builder.debugMode;
	}
}
