package io.openems.edge.controller.ess.sohcycle;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String essId;
		private Mode mode;
		private boolean referenceCycleEnabled;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setReferenceCycleEnabled(boolean referenceCycleEnabled) {
			this.referenceCycleEnabled = referenceCycleEnabled;
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
	public String ess_id() {
		return "";
	}

	@Override
	public String ess_target() {
		return "";
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return LogVerbosity.DEBUG_LOG;
	}

	@Override
	public boolean referenceCycleEnabled() {
		return this.builder.referenceCycleEnabled;
	}
}
