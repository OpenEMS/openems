package io.openems.edge.core.appmanager;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		public String apps;

		private Builder() {
		}

		public Builder setApps(String apps) {
			this.apps = apps;
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
		super(Config.class, AppManager.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public String apps() {
		return this.builder.apps;
	}

}