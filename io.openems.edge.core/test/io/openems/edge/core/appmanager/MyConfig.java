package io.openems.edge.core.appmanager;

import java.util.Optional;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String apps;
		private String key;

		private Builder() {
		}

		public Builder setApps(String apps) {
			this.apps = apps;
			return this;
		}

		public Builder setKey(String key) {
			this.key = key;
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

	@Override
	public String keyForFreeApps() {
		return Optional.ofNullable(this.builder.key).orElse("");
	}

}
